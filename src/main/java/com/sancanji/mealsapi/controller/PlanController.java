package com.sancanji.mealsapi.controller;

import com.sancanji.mealsapi.dto.ApiResponse;
import com.sancanji.mealsapi.dto.PageResponse;
import com.sancanji.mealsapi.dto.PlanDTO;
import com.sancanji.mealsapi.service.PlanService;
import com.sancanji.mealsapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ApiResponse<PageResponse<PlanDTO.PlanListItem>> getPlans(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(planService.getPlans(userId, page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanDTO.PlanResponse> getPlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(planService.getPlan(userId, id));
    }

    @GetMapping("/today")
    public ApiResponse<PlanDTO.PlanResponse> getTodayPlan(@RequestHeader("Authorization") String authorization) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(planService.getTodayPlan(userId));
    }

    @PostMapping
    public ApiResponse<PlanDTO.PlanResponse> createPlan(
            @RequestHeader("Authorization") String authorization,
            @RequestBody PlanDTO.PlanRequest request) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success("创建成功", planService.createPlan(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<PlanDTO.PlanResponse> updatePlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody PlanDTO.UpdatePlanRequest request) {
        Long userId = getUserIdFromToken(authorization);
        request.setUserId(userId);  // 设置用户ID用于冰箱库存检查
        return ApiResponse.success("更新成功", planService.updatePlan(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePlan(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        Long userId = getUserIdFromToken(authorization);
        planService.deletePlan(userId, id);
        return ApiResponse.success("删除成功", null);
    }

    @GetMapping("/check-conflict")
    public ApiResponse<PlanDTO.ConflictResponse> checkConflict(
            @RequestHeader("Authorization") String authorization,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Long excludeId) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(planService.checkConflict(userId, startDate, endDate, excludeId));
    }

    @GetMapping("/planned-dates")
    public ApiResponse<PlanDTO.PlannedDatesResponse> getPlannedDates(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long excludeId) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(planService.getPlannedDates(userId, excludeId));
    }

    @PostMapping("/calculate-nutrition")
    public ApiResponse<PlanDTO.NutritionResponse> calculateNutrition(
            @RequestBody PlanDTO.CalculateNutritionRequest request) {
        return ApiResponse.success(planService.calculateNutrition(request.getDishIds()));
    }

    private Long getUserIdFromToken(String authorization) {
        String token = authorization.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }
}