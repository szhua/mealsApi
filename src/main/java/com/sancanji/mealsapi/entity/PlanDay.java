package com.sancanji.mealsapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

/**
 * 每日规划表
 */
@Data
@TableName("t_plan_day")
public class PlanDay {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规划ID */
    private Long planId;

    /** 日期 */
    private LocalDate date;

    /** 早餐菜品ID列表(JSON) */
    private String breakfast;

    /** 午餐菜品ID列表(JSON) */
    private String lunch;

    /** 晚餐菜品ID列表(JSON) */
    private String dinner;
}