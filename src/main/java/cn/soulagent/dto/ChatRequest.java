package cn.soulagent.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private Long characterId;
    private String message;
}
