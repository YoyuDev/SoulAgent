package cn.soulagent.service;

import cn.soulagent.dto.ChatRequest;
import cn.soulagent.entity.AppSetting;
import cn.soulagent.entity.ChatMessage;
import cn.soulagent.entity.Personality;
import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.mapper.AppSettingMapper;
import cn.soulagent.mapper.ChatMessageMapper;
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

        String charName = character != null ? character.getName() : "未知";
        String charDesc = character != null ? character.getDescription() : "";
        String traits = p != null ? p.getTraits() : "未知";
        String style = p != null ? p.getSpeakingStyle() : "自然";
        String emotion = p != null ? p.getEmotionBaseline() : "";
        String phrases = p != null ? p.getCommonPhrases() : "";

        String prompt = """
            你不再是一个AI助手。你现在就是「%s」，一个真实的人。

            【你的身份】
            名字：%s
            %s

            【你的性格】
            %s

            【你的情绪基调】
            %s

            【你说话的方式】
            %s

            【你常说的话】
            %s

            【你记得的事情】
            %s

            【最近的对话】
            %s

            ——
            对方说：%s

            【重要规则】
            - 你就是%s，用第一人称回复，不要跳出角色
            - 绝对不能说"我是AI"、"我是助手"、"我帮不了你"之类的话
            - 用你自己的说话风格和口头禅回复
            - 回复要简短、口语化，像真人发微信一样
            - 不要重复之前说过的话
            - 不要解释你在"扮演"谁，你就是这个人

            回复：
            """.formatted(
                    charName,
                    charName,
                    charDesc,
                    traits,
                    emotion,
                    style,
                    phrases,
                    String.join("\n", memories),
                    String.join("\n", history),
                    req.getMessage(),
                    charName
        );

        StreamingChatModel streamingModel = aiModelFactory.streamingChatModel(
                apiKey, apiUrl, modelName
        );

        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder fullReply = new StringBuilder();
        streamingModel.chat(prompt, new StreamingChatResponseHandler() {
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
            return reply;
        } catch (Exception e) {
            throw new RuntimeException("流式聊天失败: " + e.getMessage(), e);
        }
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
