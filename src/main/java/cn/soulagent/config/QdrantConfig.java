package cn.soulagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

@Configuration
public class QdrantConfig {

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6334}")
    private int port;

    @Value("${qdrant.rest-port:6333}")
    private int restPort;

    @Value("${qdrant.collection:memory}")
    private String collection;

    @Value("${qdrant.dimension:1024}")
    private int dimension;

    @PostConstruct
    public void initCollection() {
        try {
            RestTemplate rest = new RestTemplate();
            ObjectMapper mapper = new ObjectMapper();
            String baseUrl = "http://" + host + ":" + restPort;

            // 检查 collection 是否存在
            try {
                ResponseEntity<String> resp = rest.getForEntity(baseUrl + "/collections/" + collection, String.class);
                if (resp.getStatusCode().is2xxSuccessful()) {
                    JsonNode info = mapper.readTree(resp.getBody());
                    System.out.println("[Qdrant] Collection '" + collection + "' 已存在: " + info.get("result").get("status"));
                    return;
                }
            } catch (Exception e) {
                // collection 不存在，需要创建
            }

            // 创建 collection
            System.out.println("[Qdrant] 创建 collection: " + collection + ", dimension=" + dimension);
            String createBody = "{\"vectors\":{\"size\":" + dimension + ",\"distance\":\"Cosine\"}}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(createBody, headers);
            ResponseEntity<String> resp = rest.exchange(baseUrl + "/collections/" + collection, HttpMethod.PUT, entity, String.class);
            System.out.println("[Qdrant] 创建结果: " + resp.getBody());
        } catch (Exception e) {
            System.err.println("[Qdrant] 初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        System.out.println("[Qdrant] 使用 REST API 连接: " + host + ":" + restPort + ", collection=" + collection);
        return new QdrantRestEmbeddingStore(host, restPort, collection);
    }
}
