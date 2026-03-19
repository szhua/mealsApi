package com.sancanji.mealsapi.controller;

import com.sancanji.mealsapi.dto.ApiResponse;
import com.sancanji.mealsapi.dto.DishDTO;
import com.sancanji.mealsapi.dto.PageResponse;
import com.sancanji.mealsapi.service.DishService;
import com.sancanji.mealsapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ApiResponse<PageResponse<DishDTO.DishResponse>> getDishes(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String keyword) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(dishService.getDishes(userId, page, pageSize, category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<DishDTO.DishResponse> getDish(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(dishService.getDish(userId, id));
    }

    @PostMapping
    public ApiResponse<DishDTO.DishResponse> addDish(
            @RequestHeader("Authorization") String authorization,
            @RequestBody DishDTO.DishRequest request) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success("添加成功", dishService.addDish(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DishDTO.DishResponse> updateDish(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody DishDTO.DishRequest request) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success("更新成功", dishService.updateDish(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteDish(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        Long userId = getUserIdFromToken(authorization);
        dishService.deleteDish(userId, id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/batch")
    public ApiResponse<List<DishDTO.DishResponse>> getDishesBatch(@RequestBody DishDTO.BatchRequest request) {
        return ApiResponse.success(dishService.getDishesBatch(request.getIds()));
    }

    @GetMapping("/{id}/in-plans")
    public ApiResponse<DishDTO.InPlanResponse> checkDishInPlans(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(dishService.checkDishInPlans(userId, id));
    }

    private Long getUserIdFromToken(String authorization) {
        String token = authorization.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }
}