package cn.soulagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("character")
public class SoulCharacter {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String avatar;
}
