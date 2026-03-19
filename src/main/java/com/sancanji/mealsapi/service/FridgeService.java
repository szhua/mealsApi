package com.sancanji.mealsapi.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sancanji.mealsapi.dto.FridgeDTO;
import com.sancanji.mealsapi.entity.Dish;
import com.sancanji.mealsapi.entity.Fridge;
import com.sancanji.mealsapi.mapper.DishMapper;
import com.sancanji.mealsapi.mapper.FridgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FridgeService {

    private final FridgeMapper fridgeMapper;
    private final DishMapper dishMapper;

    /**
     * 获取用户冰箱列表
     */
    public List<FridgeDTO.FridgeItem> getFridge(Long userId) {
        LambdaQueryWrapper<Fridge> query = new LambdaQueryWrapper<>();
        query.eq(Fridge::getUserId, userId).orderByDesc(Fridge::getUpdatedAt);
        List<Fridge> fridgeItems = fridgeMapper.selectList(query);

        if (fridgeItems.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取菜品详情
        List<Long> dishIds = fridgeItems.stream().map(Fridge::getDishId).collect(Collectors.toList());
        List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
        Map<Long, Dish> dishMap = dishes.stream().collect(Collectors.toMap(Dish::getId, d -> d));

        return fridgeItems.stream().map(item -> {
            FridgeDTO.FridgeItem dto = new FridgeDTO.FridgeItem();
            dto.setId(item.getId());
            dto.setDishId(item.getDishId());
            dto.setQuantity(item.getQuantity());
            dto.setUnit(item.getUnit());
            dto.setExpiryDate(item.getExpiryDate());

            Dish dish = dishMap.get(item.getDishId());
            if (dish != null) {
                dto.setDishName(dish.getName());
                dto.setDishImage(dish.getImage());
                dto.setCategoryId(dish.getCategoryId());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 添加菜品到冰箱
     */
    @Transactional
    public FridgeDTO.FridgeItem addToFridge(Long userId, FridgeDTO.AddRequest request) {
        // 检查是否已存在
        LambdaQueryWrapper<Fridge> query = new LambdaQueryWrapper<>();
        query.eq(Fridge::getUserId, userId).eq(Fridge::getDishId, request.getDishId());
        Fridge existing = fridgeMapper.selectOne(query);

        if (existing != null) {
            // 已存在，增加数量
            existing.setQuantity(existing.getQuantity() + (request.getQuantity() != null ? request.getQuantity() : 1));
            existing.setUpdatedAt(LocalDateTime.now());
            if (request.getExpiryDate() != null) {
                existing.setExpiryDate(request.getExpiryDate());
            }
            fridgeMapper.updateById(existing);
            return toItem(existing);
        }

        // 不存在，新建
        Fridge fridge = new Fridge();
        fridge.setUserId(userId);
        fridge.setDishId(request.getDishId());
        fridge.setQuantity(request.getQuantity() != null ? request.getQuantity() : 1);
        fridge.setUnit(StrUtil.isNotBlank(request.getUnit()) ? request.getUnit() : "份");
        fridge.setExpiryDate(request.getExpiryDate());
        fridge.setCreatedAt(LocalDateTime.now());
        fridge.setUpdatedAt(LocalDateTime.now());
        fridgeMapper.insert(fridge);

        return toItem(fridge);
    }

    /**
     * 更新冰箱中的菜品数量
     */
    @Transactional
    public FridgeDTO.FridgeItem updateQuantity(Long userId, Long id, FridgeDTO.UpdateRequest request) {
        LambdaQueryWrapper<Fridge> query = new LambdaQueryWrapper<>();
        query.eq(Fridge::getId, id).eq(Fridge::getUserId, userId);
        Fridge fridge = fridgeMapper.selectOne(query);

        if (fridge == null) {
            throw new RuntimeException("冰箱中没有该菜品");
        }

        fridge.setQuantity(request.getQuantity());
        fridge.setUpdatedAt(LocalDateTime.now());
        if (request.getExpiryDate() != null) {
            fridge.setExpiryDate(request.getExpiryDate());
        }
        fridgeMapper.updateById(fridge);

        return toItem(fridge);
    }

    /**
     * 从冰箱移除菜品
     */
    @Transactional
    public void removeFromFridge(Long userId, Long id) {
        LambdaQueryWrapper<Fridge> query = new LambdaQueryWrapper<>();
        query.eq(Fridge::getId, id).eq(Fridge::getUserId, userId);
        fridgeMapper.delete(query);
    }

    /**
     * 检查并扣减冰箱库存（带乐观锁重试）
     * @return true 表示扣减成功，false 表示库存不足
     */
    @Transactional
    public boolean consumeDish(Long userId, Long dishId, int quantity) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            LambdaQueryWrapper<Fridge> query = new LambdaQueryWrapper<>();
            query.eq(Fridge::getUserId, userId).eq(Fridge::getDishId, dishId);
            Fridge fridge = fridgeMapper.selectOne(query);

            if (fridge == null || fridge.getQuantity() < quantity) {
                return false;
            }

            fridge.setQuantity(fridge.getQuantity() - quantity);
            fridge.setUpdatedAt(LocalDateTime.now());

            // 使用乐观锁更新，updateById 会自动检查 version
            int rows = fridgeMapper.updateById(fridge);
            if (rows > 0) {
                return true;
            }
            // 乐观锁失败，重试
        }
        return false;
    }

    /**
     * 检查库存是否充足
     */
    public boolean checkStock(Long userId, Long dishId, int quantity) {
        LambdaQueryWrapper<Fridge> query = new LambdaQueryWrapper<>();
        query.eq(Fridge::getUserId, userId).eq(Fridge::getDishId, dishId);
        Fridge fridge = fridgeMapper.selectOne(query);
        return fridge != null && fridge.getQuantity() >= quantity;
    }

    /**
     * 获取菜品在冰箱中的数量
     */
    public int getQuantity(Long userId, Long dishId) {
        LambdaQueryWrapper<Fridge> query = new LambdaQueryWrapper<>();
        query.eq(Fridge::getUserId, userId).eq(Fridge::getDishId, dishId);
        Fridge fridge = fridgeMapper.selectOne(query);
        return fridge != null ? fridge.getQuantity() : 0;
    }

    private FridgeDTO.FridgeItem toItem(Fridge fridge) {
        FridgeDTO.FridgeItem item = new FridgeDTO.FridgeItem();
        item.setId(fridge.getId());
        item.setDishId(fridge.getDishId());
        item.setQuantity(fridge.getQuantity());
        item.setUnit(fridge.getUnit());
        item.setExpiryDate(fridge.getExpiryDate());

        Dish dish = dishMapper.selectById(fridge.getDishId());
        if (dish != null) {
            item.setDishName(dish.getName());
            item.setDishImage(dish.getImage());
            item.setCategoryId(dish.getCategoryId());
        }
        return item;
    }
}