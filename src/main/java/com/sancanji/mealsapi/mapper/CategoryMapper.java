package com.sancanji.mealsapi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sancanji.mealsapi.entity.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}