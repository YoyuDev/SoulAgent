package cn.soulagent.mapper;

import cn.soulagent.entity.SoulCharacter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CharacterMapper extends BaseMapper<SoulCharacter> {}
