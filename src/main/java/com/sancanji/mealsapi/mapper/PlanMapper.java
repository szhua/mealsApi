package com.sancanji.mealsapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sancanji.mealsapi.entity.Plan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlanMapper extends BaseMapper<Plan> {
}