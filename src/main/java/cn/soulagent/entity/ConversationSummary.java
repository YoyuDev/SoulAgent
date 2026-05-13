package cn.soulagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("conversation_summary")
public class ConversationSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long characterId;

    private String summary;

    private Long lastUpdated;

    private Long messageCount;
}