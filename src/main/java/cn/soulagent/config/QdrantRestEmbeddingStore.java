package cn.soulagent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class QdrantRestEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final Logger log = LoggerFactory.getLogger(QdrantRestEmbeddingStore.class);

    private final String baseUrl;
    private final String collectionName;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public QdrantRestEmbeddingStore(String host, int port, String collectionName) {
        this.baseUrl = "http://" + host + ":" + port;
        this.collectionName = collectionName;
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        upsert(id, embedding, null);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        upsert(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment segment) {
        String id = UUID.randomUUID().toString();
        upsert(id, embedding, segment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding e : embeddings) {
            ids.add(add(e));
        }
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> segments) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            TextSegment seg = (segments != null && i < segments.size()) ? segments.get(i) : null;
            upsert(id, embeddings.get(i), seg);
            ids.add(id);
        }
        return ids;
    }

    private void upsert(String id, Embedding embedding, TextSegment segment) {
        try {
            ObjectNode point = mapper.createObjectNode();
            point.put("id", id);

            ArrayNode vector = mapper.createArrayNode();
            for (float v : embedding.vector()) {
                vector.add(v);
            }
            point.set("vector", vector);

            ObjectNode payload = mapper.createObjectNode();
            if (segment != null) {
                payload.put("text", segment.text());
            }
            point.set("payload", payload);

            ArrayNode points = mapper.createArrayNode();
            points.add(point);

            ObjectNode body = mapper.createObjectNode();
            body.set("points", points);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            String url = baseUrl + "/collections/" + collectionName + "/points?wait=true";
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.PUT, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                System.err.println("[Qdrant REST] upsert failed: " + resp.getBody());
            }
        } catch (Exception e) {
            System.err.println("[Qdrant REST] upsert exception: " + e.getMessage());
            throw new RuntimeException("Qdrant REST upsert failed", e);
        }
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        try {
            ArrayNode vector = mapper.createArrayNode();
            for (float v : request.queryEmbedding().vector()) {
                vector.add(v);
            }

            ObjectNode body = mapper.createObjectNode();
            body.set("vector", vector);
            body.put("limit", request.maxResults());
            body.put("with_payload", true);

            Double minScore = request.minScore();
            if (minScore != null) {
                body.put("score_threshold", minScore);
                log.debug("[Qdrant REST] score_threshold={}", minScore);
            }

            Filter filter = request.filter();
            if (filter != null) {
                ObjectNode filterNode = buildFilterNode(filter);
                if (filterNode != null) {
                    body.set("filter", filterNode);
                    log.debug("[Qdrant REST] filter={}", filterNode);
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(body), headers);

            String url = baseUrl + "/collections/" + collectionName + "/points/search";
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.POST, entity, String.class);

            List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = mapper.readTree(resp.getBody());
                JsonNode result = root.get("result");
                if (result != null) {
                    for (JsonNode item : result) {
                        double score = item.has("score") ? item.get("score").asDouble() : 0.0;
                        JsonNode payload = item.get("payload");
                        String text = (payload != null && payload.has("text")) ? payload.get("text").asText() : "";
                        TextSegment seg = TextSegment.from(text);
                        EmbeddingMatch<TextSegment> match = new EmbeddingMatch<>(score, null, null, seg);
                        matches.add(match);
                    }
                }
            }

            log.debug("[Qdrant REST] search returned {} matches", matches.size());
            return new EmbeddingSearchResult<>(matches);
        } catch (Exception e) {
            System.err.println("[Qdrant REST] search exception: " + e.getMessage());
            throw new RuntimeException("Qdrant REST search failed", e);
        }
    }

    private ObjectNode buildFilterNode(Filter filter) {
        if (filter == null) return null;

        String filterStr = filter.toString();

        if (filterStr.contains("characterId")) {
            int start = filterStr.indexOf("characterId=") + "characterId=".length();
            int end = filterStr.indexOf(')', start);
            if (end > start) {
                String value = filterStr.substring(start, end).trim();
                ObjectNode filterNode = mapper.createObjectNode();
                ArrayNode must = mapper.createArrayNode();
                ObjectNode shouldMatch = mapper.createObjectNode();
                ObjectNode match = mapper.createObjectNode();
                match.put("key", "characterId");
                match.put("value", value);
                shouldMatch.set("match", match);
                must.add(shouldMatch);
                filterNode.set("must", must);
                return filterNode;
            }
        }
        return null;
    }
}
