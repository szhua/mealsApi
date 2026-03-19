package com.sancanji.mealsapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 菜品表
 */
@Data
@TableName("t_dish")
public class Dish {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 菜品名称 */
    private String name;

    /** 图片URL */
    private String image;

    /** 分类ID */
    private Long categoryId;

    /** 热量(kcal) */
    private Integer calories;

    /** 蛋白质(g) */
    private Integer protein;

    /** 碳水(g) */
    private Integer carbs;

    /** 脂肪(g) */
    private Integer fat;

    /** 适合餐次: breakfast,lunch,dinner */
    private String mealTypes;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}