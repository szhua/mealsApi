package com.sancanji.mealsapi.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sancanji.mealsapi.dto.DishDTO;
import com.sancanji.mealsapi.dto.PageResponse;
import com.sancanji.mealsapi.entity.Category;
import com.sancanji.mealsapi.entity.Dish;
import com.sancanji.mealsapi.entity.Plan;
import com.sancanji.mealsapi.entity.PlanDay;
import com.sancanji.mealsapi.mapper.CategoryMapper;
import com.sancanji.mealsapi.mapper.DishMapper;
import com.sancanji.mealsapi.mapper.PlanDayMapper;
import com.sancanji.mealsapi.mapper.PlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishMapper dishMapper;
    private final CategoryMapper categoryMapper;
    private final PlanMapper planMapper;
    private final PlanDayMapper planDayMapper;

    public PageResponse<DishDTO.DishResponse> getDishes(Long userId, int page, int pageSize,
                                                          Long categoryId, String keyword) {
        LambdaQueryWrapper<Dish> query = new LambdaQueryWrapper<>();
        query.eq(Dish::getUserId, userId);
        if (categoryId != null) {
            query.eq(Dish::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(keyword)) {
            query.like(Dish::getName, keyword);
        }
        query.orderByDesc(Dish::getCreatedAt);

        Page<Dish> dishPage = dishMapper.selectPage(new Page<>(page, pageSize), query);

        List<DishDTO.DishResponse> list = dishPage.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(list, dishPage.getTotal(), page, pageSize);
    }

    public DishDTO.DishResponse getDish(Long userId, Long id) {
        LambdaQueryWrapper<Dish> query = new LambdaQueryWrapper<>();
        query.eq(Dish::getId, id).eq(Dish::getUserId, userId);
        Dish dish = dishMapper.selectOne(query);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }
        return toResponse(dish);
    }

    @Transactional
    public DishDTO.DishResponse addDish(Long userId, DishDTO.DishRequest request) {
        Dish dish = new Dish();
        dish.setUserId(userId);
        dish.setName(request.getName());
        dish.setImage(request.getImage());
        dish.setCategoryId(request.getCategoryId());
        dish.setCalories(request.getCalories() != null ? request.getCalories() : 0);
        dish.setProtein(request.getProtein() != null ? request.getProtein() : 0);
        dish.setCarbs(request.getCarbs() != null ? request.getCarbs() : 0);
        dish.setFat(request.getFat() != null ? request.getFat() : 0);
        dish.setMealTypes(request.getMealTypes() != null ?
                String.join(",", request.getMealTypes()) : "");
        dish.setCreatedAt(LocalDateTime.now());
        dish.setUpdatedAt(LocalDateTime.now());
        dishMapper.insert(dish);
        return toResponse(dish);
    }

    @Transactional
    public DishDTO.DishResponse updateDish(Long userId, Long id, DishDTO.DishRequest request) {
        LambdaQueryWrapper<Dish> query = new LambdaQueryWrapper<>();
        query.eq(Dish::getId, id).eq(Dish::getUserId, userId);
        Dish dish = dishMapper.selectOne(query);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }
        if (request.getName() != null) {
            dish.setName(request.getName());
        }
        if (request.getImage() != null) {
            dish.setImage(request.getImage());
        }
        if (request.getCategoryId() != null) {
            dish.setCategoryId(request.getCategoryId());
        }
        if (request.getCalories() != null) {
            dish.setCalories(request.getCalories());
        }
        if (request.getProtein() != null) {
            dish.setProtein(request.getProtein());
        }
        if (request.getCarbs() != null) {
            dish.setCarbs(request.getCarbs());
        }
        if (request.getFat() != null) {
            dish.setFat(request.getFat());
        }
        if (request.getMealTypes() != null) {
            dish.setMealTypes(String.join(",", request.getMealTypes()));
        }
        dish.setUpdatedAt(LocalDateTime.now());
        dishMapper.updateById(dish);
        return toResponse(dish);
    }

    @Transactional
    public void deleteDish(Long userId, Long id) {
        LambdaQueryWrapper<Dish> query = new LambdaQueryWrapper<>();
        query.eq(Dish::getId, id).eq(Dish::getUserId, userId);
        Dish dish = dishMapper.selectOne(query);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }
        dishMapper.deleteById(id);
    }

    public List<DishDTO.DishResponse> getDishesBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Dish> dishes = dishMapper.selectBatchIds(ids);
        return dishes.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public DishDTO.InPlanResponse checkDishInPlans(Long userId, Long dishId) {
        // 查找用户的所有规划
        LambdaQueryWrapper<Plan> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(Plan::getUserId, userId);
        List<Plan> plans = planMapper.selectList(planQuery);

        for (Plan plan : plans) {
            LambdaQueryWrapper<PlanDay> dayQuery = new LambdaQueryWrapper<>();
            dayQuery.eq(PlanDay::getPlanId, plan.getId());
            List<PlanDay> planDays = planDayMapper.selectList(dayQuery);
            for (PlanDay day : planDays) {
                List<Long> breakfast = parseDishIds(day.getBreakfast());
                List<Long> lunch = parseDishIds(day.getLunch());
                List<Long> dinner = parseDishIds(day.getDinner());
                if (breakfast.contains(dishId) || lunch.contains(dishId) || dinner.contains(dishId)) {
                    DishDTO.InPlanResponse response = new DishDTO.InPlanResponse();
                    response.setInPlan(true);
                    response.setPlanDateRange(plan.getStartDate() + " ~ " + plan.getEndDate());
                    return response;
                }
            }
        }

        DishDTO.InPlanResponse response = new DishDTO.InPlanResponse();
        response.setInPlan(false);
        return response;
    }

    private List<Long> parseDishIds(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, Long.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private DishDTO.DishResponse toResponse(Dish dish) {
        DishDTO.DishResponse response = new DishDTO.DishResponse();
        response.setId(dish.getId());
        response.setName(dish.getName());
        response.setImage(dish.getImage());
        response.setCategoryId(dish.getCategoryId());
        response.setCalories(dish.getCalories());
        response.setProtein(dish.getProtein());
        response.setCarbs(dish.getCarbs());
        response.setFat(dish.getFat());
        response.setMealTypes(StrUtil.isNotBlank(dish.getMealTypes()) ?
                List.of(dish.getMealTypes().split(",")) : new ArrayList<>());
        response.setCreatedAt(dish.getCreatedAt() != null ?
                dish.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null);

        // 查询分类名称
        if (dish.getCategoryId() != null) {
            Category category = categoryMapper.selectById(dish.getCategoryId());
            if (category != null) {
                response.setCategoryName(category.getName());
            }
        }
        return response;
    }
}