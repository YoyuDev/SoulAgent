package cn.soulagent.service;

import cn.soulagent.dto.ChatRequest;
import cn.soulagent.entity.ChatMessage;
import cn.soulagent.entity.Personality;
import cn.soulagent.entity.SoulCharacter;
import cn.soulagent.mapper.ChatMessageMapper;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final PersonalityService personalityService;
    private final CharacterService characterService;
    private final MemoryService memoryService;
    private final RedisService redisService;
    private final AiModelFactory aiModelFactory;
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 流式聊天，通过回调逐个发送 token
     */
    public String chatStream(ChatRequest req, Consumer<String> onToken, Runnable onComplete) {

        SoulCharacter character = characterService.getById(req.getCharacterId());
        Personality p = personalityService.get(req.getCharacterId());
        List<String> history = redisService.get(req.getCharacterId());

        List<String> memories;
        try {
            memories = memoryService.search(
                    req.getCharacterId(),
                    req.getMessage(),
                    req.getApiKey(),
                    req.getApiUrl(),
                    req.getEmbeddingApiKey(),
                    req.getEmbeddingApiUrl(),
                    req.getEmbeddingModelName()
            );
        } catch (Exception e) {
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
                req.getApiKey(), req.getApiUrl(), req.getModelName()
        );

        // 使用流式输出
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

    private void saveMessage(Long characterId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setCharacterId(characterId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreateTime(System.currentTimeMillis());
        chatMessageMapper.insert(msg);
    }
}
