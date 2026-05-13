package cn.soulagent.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long characterId;
    private String message;
    private String apiUrl;
    private String apiKey;
    private String modelName;
    private String embeddingApiUrl;
    private String embeddingApiKey;
    private String embeddingModelName;
}
