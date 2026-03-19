package com.sancanji.mealsapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sancanji.mealsapi.entity.Streak;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StreakMapper extends BaseMapper<Streak> {
}