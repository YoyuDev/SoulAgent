package cn.soulagent.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final AiModelFactory aiModelFactory;

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

                // 必须传 TextSegment，否则 Qdrant 收到空向量
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
}
