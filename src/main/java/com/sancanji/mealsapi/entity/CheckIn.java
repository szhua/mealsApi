package com.sancanji.mealsapi.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

/**
 * 打卡历史记录表
 */
@Data
@TableName("t_check_in")
public class CheckIn {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 打卡日期 */
    private LocalDate checkDate;
}