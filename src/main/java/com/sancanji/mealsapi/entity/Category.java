package com.sancanji.mealsapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 菜品分类表
 */
@Data
@TableName("t_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类编码 */
    private String code;

    /** 分类名称 */
    private String name;

    /** 图标 */
    private String icon;

    /** 排序 */
    private Integer sort;
}