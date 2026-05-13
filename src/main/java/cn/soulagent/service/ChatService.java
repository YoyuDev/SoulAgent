package cn.soulagent.service;

import cn.soulagent.dto.ChatRequest;
import cn.soulagent.entity.AppSetting;
import cn.soulagent.entity.ChatMessage;
import cn.soulagent.entity.Personality;
import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.mapper.AppSettingMapper;
import cn.soulagent.mapper.ChatMessageMapper;
import cn.soulagent.skill.SkillContext;
import cn.soulagent.skill.SkillResult;
import cn.soulagent.skill.SkillRouter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final PersonalityService personalityService;
    private final CharacterService characterService;
    private final MemoryService memoryService;
    private final RedisService redisService;
    private final AiModelFactory aiModelFactory;
    private final ChatMessageMapper chatMessageMapper;
    private final AppSettingMapper appSettingMapper;
    private final SkillRouter skillRouter;
    private final ExecutorService taskExecutor;

    public String chatStream(ChatRequest req, Consumer<String> onToken, Runnable onComplete) {

        String apiKey = getSetting("apiKey");
        String apiUrl = getSetting("apiUrl");
        String modelName = getSetting("modelName");
        String embeddingApiKey = getSetting("embeddingApiKey");
        String embeddingApiUrl = getSetting("embeddingApiUrl");
        String embeddingModelName = getSetting("embeddingModelName");

        SoulCharacter character = characterService.getById(req.getCharacterId());
        Personality p = personalityService.get(req.getCharacterId());
        List<String> history = redisService.get(req.getCharacterId());

        String summary = memoryService.getConversationSummary(req.getCharacterId());

        List<String> memories;
        try {
            memories = memoryService.search(
                    req.getCharacterId(),
                    req.getMessage(),
                    apiKey,
                    apiUrl,
                    embeddingApiKey,
                    embeddingApiUrl,
                    embeddingModelName
            );
        } catch (Exception e) {
            log.warn("记忆检索失败: {}", e.getMessage());
            memories = List.of();
        }

        SkillContext ctx = SkillContext.builder()
                .characterId(req.getCharacterId())
                .character(character)
                .personality(p)
                .userMessage(req.getMessage())
                .recentHistory(history)
                .memories(memories)
                .summary(summary)
                .apiKey(apiKey)
                .apiUrl(apiUrl)
                .modelName(modelName)
                .embeddingApiKey(embeddingApiKey)
                .embeddingApiUrl(embeddingApiUrl)
                .embeddingModelName(embeddingModelName)
                .build();

        SkillResult result = skillRouter.route(ctx);

        if (result.isDirectReply()) {
            String reply = result.getDirectContent();
            onToken.accept(reply);
            onComplete.run();
            saveMessage(req.getCharacterId(), "user", req.getMessage());
            saveMessage(req.getCharacterId(), "assistant", reply);
            redisService.append(req.getCharacterId(), req.getMessage(), reply);
            return reply;
        }

        return callLlm(req, result, onToken, onComplete, apiKey, apiUrl, modelName, p);
    }

    private String callLlm(ChatRequest req, SkillResult result,
                           Consumer<String> onToken, Runnable onComplete,
                           String apiKey, String apiUrl, String modelName,
                           Personality personality) {

        StreamingChatModel streamingModel = aiModelFactory.streamingChatModel(
                apiKey, apiUrl, modelName
        );

        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder fullReply = new StringBuilder();
        streamingModel.chat(result.getPrompt(), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                fullReply.append(partialResponse);
                onToken.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                onComplete.run();
                future.complete(fullReply.toString());
            }

            @Override
            public void onError(Throwable error) {
                log.error("流式聊天失败: {}", error.getMessage());
                onComplete.run();
                future.completeExceptionally(error);
            }
        });

        try {
            String reply = future.get();
            redisService.append(req.getCharacterId(), req.getMessage(), reply);
            saveMessage(req.getCharacterId(), "user", req.getMessage());
            saveMessage(req.getCharacterId(), "assistant", reply);

            asyncUpdateEmotion(req.getCharacterId(), req.getMessage(), reply, personality,
                    apiKey, apiUrl, modelName);

            asyncCheckAndSummarize(req.getCharacterId(), apiKey, apiUrl, modelName);

            return reply;
        } catch (Exception e) {
            throw new RuntimeException("流式聊天失败: " + e.getMessage(), e);
        }
    }

    private void asyncUpdateEmotion(Long characterId, String userMessage, String aiReply,
                                     Personality personality,
                                     String apiKey, String apiUrl, String modelName) {
        taskExecutor.submit(() -> {
            try {
                String emotionBaseline = personality != null ? personality.getEmotionBaseline() : "";
                String newEmotion = personalityService.analyzeEmotion(
                        userMessage, aiReply, emotionBaseline, apiKey, apiUrl, modelName);
                if (newEmotion != null && !newEmotion.isEmpty()) {
                    personalityService.updateEmotion(characterId, newEmotion);
                    log.info("角色 {} 情绪更新: {}", characterId, newEmotion);
                }
            } catch (Exception e) {
                log.warn("情绪更新失败: {}", e.getMessage());
            }
        });
    }

    private void asyncCheckAndSummarize(Long characterId, String apiKey, String apiUrl, String modelName) {
        taskExecutor.submit(() -> {
            try {
                memoryService.checkAndSummarize(characterId, apiKey, apiUrl, modelName);
            } catch (Exception e) {
                log.warn("记忆压缩失败: {}", e.getMessage());
            }
        });
    }

    private String getSetting(String key) {
        AppSetting setting = appSettingMapper.selectOne(
                new QueryWrapper<AppSetting>().eq("setting_key", key)
        );
        return setting != null ? setting.getSettingValue() : null;
    }

    private void saveMessage(Long characterId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setCharacterId(characterId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreateTime(System.currentTimeMillis());
        chatMessageMapper.insert(msg);
    }
}
