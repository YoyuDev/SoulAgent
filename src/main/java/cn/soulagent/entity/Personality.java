package cn.soulagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("personality")
public class Personality {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long characterId;

    private String traits;
    private String speakingStyle;
    private String emotionBaseline;
    private String commonPhrases;
    private String currentEmotion;
    private Long conversationCount;
}
