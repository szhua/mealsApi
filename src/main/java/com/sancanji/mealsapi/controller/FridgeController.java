package com.sancanji.mealsapi.controller;

import com.sancanji.mealsapi.dto.ApiResponse;
import com.sancanji.mealsapi.dto.FridgeDTO;
import com.sancanji.mealsapi.service.FridgeService;
import com.sancanji.mealsapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fridge")
@RequiredArgsConstructor
public class FridgeController {

    private final FridgeService fridgeService;
    private final JwtUtil jwtUtil;

    /**
     * 获取冰箱列表
     */
    @GetMapping
    public ApiResponse<List<FridgeDTO.FridgeItem>> getFridge(@RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
        List<FridgeDTO.FridgeItem> items = fridgeService.getFridge(userId);
        return ApiResponse.success(items);
    }

    /**
     * 添加菜品到冰箱
     */
    @PostMapping
    public ApiResponse<FridgeDTO.FridgeItem> addToFridge(
            @RequestHeader("Authorization") String token,
            @RequestBody FridgeDTO.AddRequest request) {
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
        FridgeDTO.FridgeItem item = fridgeService.addToFridge(userId, request);
        return ApiResponse.success(item);
    }

    /**
     * 更新冰箱中的菜品数量
     */
    @PutMapping("/{id}")
    public ApiResponse<FridgeDTO.FridgeItem> updateQuantity(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody FridgeDTO.UpdateRequest request) {
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
        FridgeDTO.FridgeItem item = fridgeService.updateQuantity(userId, id, request);
        return ApiResponse.success(item);
    }

    /**
     * 从冰箱移除菜品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeFromFridge(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
        fridgeService.removeFromFridge(userId, id);
        return ApiResponse.success(null);
    }

    /**
     * 检查库存
     */
    @GetMapping("/stock/{dishId}")
    public ApiResponse<Integer> checkStock(
            @RequestHeader("Authorization") String token,
            @PathVariable Long dishId) {
        Long userId = jwtUtil.getUserId(token.replace("Bearer ", ""));
        int quantity = fridgeService.getQuantity(userId, dishId);
        return ApiResponse.success(quantity);
    }
}