package cn.soulagent.dto;

import lombok.Data;

@Data
public class CreateCharacterRequest {
    private String name;
    private String description;
    private String chatData;
}
