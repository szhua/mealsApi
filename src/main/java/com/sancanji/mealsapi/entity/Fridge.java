package com.sancanji.mealsapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 冰箱表（用户食材库存）
 */
@Data
@TableName("t_fridge")
public class Fridge {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 菜品ID */
    private Long dishId;

    /** 数量 */
    private Integer quantity;

    /** 单位（默认：份） */
    private String unit;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 乐观锁版本号 */
    @Version
    private Integer version;
}