package com.sancanji.mealsapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sancanji.mealsapi.dto.StreakDTO;
import com.sancanji.mealsapi.entity.CheckIn;
import com.sancanji.mealsapi.entity.Streak;
import com.sancanji.mealsapi.mapper.CheckInMapper;
import com.sancanji.mealsapi.mapper.StreakMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakMapper streakMapper;
    private final CheckInMapper checkInMapper;

    public StreakDTO.StreakResponse getStreak(Long userId) {
        LambdaQueryWrapper<Streak> query = new LambdaQueryWrapper<>();
        query.eq(Streak::getUserId, userId);
        Streak streak = streakMapper.selectOne(query);

        if (streak == null) {
            StreakDTO.StreakResponse response = new StreakDTO.StreakResponse();
            response.setCurrent(0);
            response.setLongest(0);
            response.setLastDate(null);
            response.setCheckIns(List.of());
            return response;
        }

        // 查询打卡历史
        LambdaQueryWrapper<CheckIn> checkInQuery = new LambdaQueryWrapper<>();
        checkInQuery.eq(CheckIn::getUserId, userId).orderByDesc(CheckIn::getCheckDate);
        List<CheckIn> checkIns = checkInMapper.selectList(checkInQuery);

        StreakDTO.StreakResponse response = new StreakDTO.StreakResponse();
        response.setCurrent(streak.getCurrent());
        response.setLongest(streak.getLongest());
        response.setLastDate(streak.getLastDate() != null ? streak.getLastDate().toString() : null);
        response.setCheckIns(checkIns.stream()
                .map(c -> c.getCheckDate().toString())
                .collect(Collectors.toList()));
        return response;
    }

    @Transactional
    public StreakDTO.StreakResponse checkIn(Long userId, String dateStr) {
        LocalDate today = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 获取或创建打卡记录
        LambdaQueryWrapper<Streak> streakQuery = new LambdaQueryWrapper<>();
        streakQuery.eq(Streak::getUserId, userId);
        Streak streak = streakMapper.selectOne(streakQuery);

        if (streak == null) {
            streak = new Streak();
            streak.setUserId(userId);
            streak.setCurrent(0);
            streak.setLongest(0);
            streak.setCreatedAt(LocalDateTime.now());
            streak.setUpdatedAt(LocalDateTime.now());
            streakMapper.insert(streak);
        }

        // 检查今天是否已经打卡
        LambdaQueryWrapper<CheckIn> checkInQuery = new LambdaQueryWrapper<>();
        checkInQuery.eq(CheckIn::getUserId, userId).eq(CheckIn::getCheckDate, today);
        CheckIn existingCheckIn = checkInMapper.selectOne(checkInQuery);

        if (existingCheckIn == null) {
            // 新打卡
            CheckIn checkIn = new CheckIn();
            checkIn.setUserId(userId);
            checkIn.setCheckDate(today);
            checkInMapper.insert(checkIn);

            // 更新连续天数
            if (streak.getLastDate() != null && streak.getLastDate().equals(yesterday)) {
                streak.setCurrent(streak.getCurrent() + 1);
            } else {
                streak.setCurrent(1);
            }

            if (streak.getCurrent() > streak.getLongest()) {
                streak.setLongest(streak.getCurrent());
            }

            streak.setLastDate(today);
            streak.setUpdatedAt(LocalDateTime.now());
            streakMapper.updateById(streak);
        }

        return getStreak(userId);
    }
}