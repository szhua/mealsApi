package com.sancanji.mealsapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 打卡记录表
 */
@Data
@TableName("t_streak")
public class Streak {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 当前连续天数 */
    private Integer current;

    /** 最长连续天数 */
    private Integer longest;

    /** 最后打卡日期 */
    private LocalDate lastDate;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}