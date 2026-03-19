package com.sancanji.mealsapi.controller;

import com.sancanji.mealsapi.dto.ApiResponse;
import com.sancanji.mealsapi.dto.StreakDTO;
import com.sancanji.mealsapi.service.StreakService;
import com.sancanji.mealsapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/streak")
@RequiredArgsConstructor
public class StreakController {

    private final StreakService streakService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ApiResponse<StreakDTO.StreakResponse> getStreak(@RequestHeader("Authorization") String authorization) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(streakService.getStreak(userId));
    }

    @PostMapping("/check-in")
    public ApiResponse<StreakDTO.StreakResponse> checkIn(
            @RequestHeader("Authorization") String authorization,
            @RequestBody(required = false) StreakDTO.CheckInRequest request) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success("打卡成功", streakService.checkIn(userId, request != null ? request.getDate() : null));
    }

    private Long getUserIdFromToken(String authorization) {
        String token = authorization.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }
}