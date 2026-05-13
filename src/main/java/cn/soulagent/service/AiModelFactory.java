package cn.soulagent.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiModelFactory {

    @Value("${langchain4j.open-ai.chat-model.api-key:}")
    private String defaultApiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name:gpt-4o-mini}")
    private String defaultModelName;

    private String resolveApiKey(String apiKey) {
        return (apiKey != null && !apiKey.isEmpty()) ? apiKey : defaultApiKey;
    }

    private String resolveModelName(String modelName) {
        return (modelName != null && !modelName.isEmpty()) ? modelName : defaultModelName;
    }

    private String resolveApiUrl(String apiUrl) {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            // 确保以 /v1 结尾
            String url = apiUrl.replaceAll("/+$", "");
            if (!url.endsWith("/v1")) url += "/v1";
            return url;
        }
        return "https://api.openai.com/v1";
    }

    public ChatModel chatModel(String apiKey, String apiUrl, String modelName) {
        var builder = OpenAiChatModel.builder()
                .apiKey(resolveApiKey(apiKey))
                .modelName(resolveModelName(modelName))
                .timeout(java.time.Duration.ofSeconds(120))
                .maxTokens(2000);

        String url = resolveApiUrl(apiUrl);
        if (!"https://api.openai.com/v1".equals(url)) {
            builder.baseUrl(url);
        }

        return builder.build();
    }

    public StreamingChatModel streamingChatModel(String apiKey, String apiUrl, String modelName) {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(resolveApiKey(apiKey))
                .modelName(resolveModelName(modelName));

        String url = resolveApiUrl(apiUrl);
        if (!"https://api.openai.com/v1".equals(url)) {
            builder.baseUrl(url);
        }

        return builder.build();
    }

    /**
     * 创建支持 JSON 结构化输出的 ChatModel
     */
    public ChatModel jsonChatModel(String apiKey, String apiUrl, String modelName) {
        var builder = OpenAiChatModel.builder()
                .apiKey(resolveApiKey(apiKey))
                .modelName(resolveModelName(modelName))
                .timeout(java.time.Duration.ofSeconds(120))
                .maxTokens(2000)
                .responseFormat("json_object");

        String url = resolveApiUrl(apiUrl);
        if (!"https://api.openai.com/v1".equals(url)) {
            builder.baseUrl(url);
        }

        return builder.build();
    }

    public EmbeddingModel embeddingModel(String apiKey, String apiUrl,
                                          String embeddingApiKey, String embeddingApiUrl,
                                          String embeddingModelName) {
        // 向量模型独立配置，为空则回退到对话模型配置
        String ak = (embeddingApiKey != null && !embeddingApiKey.isEmpty()) ? embeddingApiKey : apiKey;
        String url = (embeddingApiUrl != null && !embeddingApiUrl.isEmpty()) ? embeddingApiUrl : apiUrl;
        String modelName = (embeddingModelName != null && !embeddingModelName.isEmpty())
                ? embeddingModelName : "BAAI/bge-m3";

        var builder = OpenAiEmbeddingModel.builder()
                .apiKey(resolveApiKey(ak))
                .modelName(modelName)
                .logRequests(true)
                .logResponses(true);

        String resolvedUrl = resolveApiUrl(url);
        System.out.println("[Embedding] baseUrl=" + resolvedUrl + ", model=" + modelName);
        if (!"https://api.openai.com/v1".equals(resolvedUrl)) {
            builder.baseUrl(resolvedUrl);
        }

        return builder.build();
    }
}
