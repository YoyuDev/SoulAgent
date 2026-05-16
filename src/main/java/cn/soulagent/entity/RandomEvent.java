package cn.soulagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("random_event")
public class RandomEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long characterId;

    private String eventType;

    private String eventContent;

    private Long eventTime;

    private Integer isShared;

    private Long shareTime;
}