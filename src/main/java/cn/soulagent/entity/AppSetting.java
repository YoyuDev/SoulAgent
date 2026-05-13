package cn.soulagent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("app_setting")
public class AppSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String settingKey;
    private String settingValue;
}
