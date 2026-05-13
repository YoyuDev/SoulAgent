package cn.soulagent.service;

import cn.soulagent.entity.ConversationSummary;
import cn.soulagent.mapper.ConversationSummaryMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private static final int SUMMARY_TRIGGER_THRESHOLD = 30;
    private static final int KEEP_AFTER_SUMMARY = 15;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AiModelFactory aiModelFactory;
    private final ConversationSummaryMapper summaryMapper;
    private final RedisService redisService;

    public void store(Long cid, List<String> msgs,
                      String apiKey, String apiUrl,
                      String embeddingApiKey, String embeddingApiUrl,
                      String embeddingModelName) {

        String ak = (embeddingApiKey != null && !embeddingApiKey.isEmpty()) ? embeddingApiKey : apiKey;
        String url = (embeddingApiUrl != null && !embeddingApiUrl.isEmpty()) ? embeddingApiUrl : apiUrl;
        String model = (embeddingModelName != null && !embeddingModelName.isEmpty()) ? embeddingModelName : "BAAI/bge-m3";

        System.out.println("[Memory] Embedding config - URL: " + url + ", Model: " + model + ", Key: " + (ak != null && !ak.isEmpty() ? ak.substring(0, Math.min(8, ak.length())) + "..." : "empty"));

        EmbeddingModel embeddingModel = aiModelFactory.embeddingModel(
                apiKey, apiUrl, embeddingApiKey, embeddingApiUrl, embeddingModelName);

        for (String msg : msgs) {

            if (msg.length() < 5) continue;

            try {
                Embedding embedding = embeddingModel.embed(msg).content();

                System.out.println("[Memory] Embedding dim=" + embedding.vector().length + ", msg=" + msg.substring(0, Math.min(20, msg.length())));

                if (embedding.vector() == null || embedding.vector().length == 0) {
                    System.err.println("[Memory] Embedding 返回空向量，模型: " + model);
                    break;
                }

                TextSegment segment = TextSegment.from(msg);
                embeddingStore.add(embedding, segment);
            } catch (Exception e) {
                System.err.println("[Memory] Embedding 调用失败: " + e.getMessage());
                e.printStackTrace();
                break;
            }
        }
    }

    public List<String> search(Long cid, String query,
                               String apiKey, String apiUrl,
                               String embeddingApiKey, String embeddingApiUrl,
                               String embeddingModelName) {

        EmbeddingModel embeddingModel = aiModelFactory.embeddingModel(
                apiKey, apiUrl, embeddingApiKey, embeddingApiUrl, embeddingModelName);

        Embedding queryEmbedding = embeddingModel.embed(query).content();

        var result = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(queryEmbedding)
                        .maxResults(5)
                        .minScore(0.6)
                        .filter((Filter) Map.of("characterId", cid))
                        .build()
        );

        return result.matches().stream()
                .map(m -> m.embedded().text())
                .toList();
    }

    public boolean checkAndSummarize(Long cid, String apiKey, String apiUrl, String modelName) {
        List<String> history = redisService.get(cid);
        if (history.size() < SUMMARY_TRIGGER_THRESHOLD) {
            return false;
        }

        List<String> toSummarize = history.subList(0, history.size() - KEEP_AFTER_SUMMARY);
        List<String> toKeep = history.subList(history.size() - KEEP_AFTER_SUMMARY, history.size());

        String existingSummary = getSummary(cid);
        String newSummary = generateSummary(toSummarize, existingSummary, apiKey, apiUrl, modelName);

        saveSummary(cid, newSummary, history.size());

        String key = "chat:" + cid;
        redisService.clear(cid);
        for (String line : toKeep) {
            redisService.getRedisTemplate().opsForList().rightPush(key, line);
        }

        log.info("角色 {} 完成记忆压缩: {} 条 -> {} 条 + 摘要 (总消息数: {})",
                cid, toSummarize.size(), KEEP_AFTER_SUMMARY, history.size());
        return true;
    }

    private String generateSummary(List<String> messages, String existingSummary,
                                    String apiKey, String apiUrl, String modelName) {

        String prompt;
        if (existingSummary != null && !existingSummary.isEmpty()) {
            prompt = """
                你是一个对话摘要专家。请合并以下两段内容，生成一段连贯的对话历史摘要：

                【已有摘要】
                %s

                【新对话】
                %s

                要求：
                - 保留重要事件、用户说过的关键信息（生日、喜好、重要决定等）
                - 保留角色和用户之间的关系进展
                - 保持时间顺序
                - 控制在 300 字以内
                - 只输出摘要，不要解释
                """.formatted(existingSummary, String.join("\n", messages));
        } else {
            prompt = """
                你是一个对话摘要专家。请将以下对话压缩成一段精炼的摘要：

                【对话内容】
                %s

                要求：
                - 保留重要事件、用户说过的关键信息（生日、喜好、重要决定等）
                - 保留角色和用户之间的关系进展
                - 保持时间顺序
                - 控制在 300 字以内
                - 只输出摘要，不要解释
                """.formatted(String.join("\n", messages));
        }

        ChatModel model = aiModelFactory.chatModel(apiKey, apiUrl, modelName);
        try {
            String res = model.chat(UserMessage.from(prompt)).aiMessage().text();
            return res != null ? res.trim() : "";
        } catch (Exception e) {
            log.warn("摘要生成失败: {}", e.getMessage());
            return existingSummary != null ? existingSummary : "";
        }
    }

    private String getSummary(Long cid) {
        ConversationSummary summary = summaryMapper.selectOne(
                new QueryWrapper<ConversationSummary>().eq("character_id", cid)
        );
        return summary != null ? summary.getSummary() : "";
    }

    private void saveSummary(Long cid, String summary, int totalMessages) {
        ConversationSummary existing = summaryMapper.selectOne(
                new QueryWrapper<ConversationSummary>().eq("character_id", cid)
        );
        if (existing != null) {
            existing.setSummary(summary);
            existing.setLastUpdated(System.currentTimeMillis());
            existing.setMessageCount((long) totalMessages);
            summaryMapper.updateById(existing);
        } else {
            ConversationSummary s = new ConversationSummary();
            s.setCharacterId(cid);
            s.setSummary(summary);
            s.setLastUpdated(System.currentTimeMillis());
            s.setMessageCount((long) totalMessages);
            summaryMapper.insert(s);
        }
    }

    public String getConversationSummary(Long cid) {
        ConversationSummary summary = summaryMapper.selectOne(
                new QueryWrapper<ConversationSummary>().eq("character_id", cid)
        );
        return summary != null ? summary.getSummary() : null;
    }
}
