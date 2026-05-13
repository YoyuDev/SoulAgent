package cn.soulagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("character_relationship")
public class CharacterRelationship {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long characterId;

    private Double intimacyScore;

    private Double trustScore;

    private Long firstChatTime;

    private Long lastChatTime;

    private Long totalMessages;

    private String relationshipStage;
}