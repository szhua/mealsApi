package com.sancanji.mealsapi.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sancanji.mealsapi.dto.FridgeDTO;
import com.sancanji.mealsapi.dto.PageResponse;
import com.sancanji.mealsapi.dto.PlanDTO;
import com.sancanji.mealsapi.entity.Dish;
import com.sancanji.mealsapi.entity.Plan;
import com.sancanji.mealsapi.entity.PlanDay;
import com.sancanji.mealsapi.mapper.DishMapper;
import com.sancanji.mealsapi.mapper.PlanDayMapper;
import com.sancanji.mealsapi.mapper.PlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanMapper planMapper;
    private final PlanDayMapper planDayMapper;
    private final DishMapper dishMapper;
    private final FridgeService fridgeService;

    public PageResponse<PlanDTO.PlanListItem> getPlans(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Plan> query = new LambdaQueryWrapper<>();
        query.eq(Plan::getUserId, userId).orderByDesc(Plan::getCreatedAt);

        Page<Plan> planPage = planMapper.selectPage(new Page<>(page, pageSize), query);

        List<PlanDTO.PlanListItem> list = planPage.getRecords().stream()
                .map(this::toListItem)
                .collect(Collectors.toList());

        return PageResponse.of(list, planPage.getTotal(), page, pageSize);
    }

    public PlanDTO.PlanResponse getPlan(Long userId, Long id) {
        LambdaQueryWrapper<Plan> query = new LambdaQueryWrapper<>();
        query.eq(Plan::getId, id).eq(Plan::getUserId, userId);
        Plan plan = planMapper.selectOne(query);
        if (plan == null) {
            throw new RuntimeException("规划不存在");
        }
        return toResponse(plan);
    }

    public PlanDTO.PlanResponse getTodayPlan(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Plan> query = new LambdaQueryWrapper<>();
        query.eq(Plan::getUserId, userId)
                .le(Plan::getStartDate, today)
                .ge(Plan::getEndDate, today);
        Plan plan = planMapper.selectOne(query);
        if (plan == null) {
            return null;
        }
        return toResponse(plan);
    }

    @Transactional
    public PlanDTO.PlanResponse createPlan(Long userId, PlanDTO.PlanRequest request) {
        // 检查日期冲突
        List<Plan> conflicts = findConflicts(userId, request.getStartDate(), request.getEndDate(), null);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("日期范围与现有规划冲突");
        }

        // 创建规划
        Plan plan = new Plan();
        plan.setUserId(userId);
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setWeeklyReset(request.getWeeklyReset() != null ? request.getWeeklyReset() : false);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.insert(plan);

        // 初始化每日数据
        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        for (int i = 0; i < days; i++) {
            LocalDate date = request.getStartDate().plusDays(i);
            PlanDay planDay = new PlanDay();
            planDay.setPlanId(plan.getId());
            planDay.setDate(date);
            planDay.setBreakfast("[]");
            planDay.setLunch("[]");
            planDay.setDinner("[]");
            planDayMapper.insert(planDay);
        }

        return toResponse(plan);
    }

    @Transactional
    public PlanDTO.PlanResponse updatePlan(Long userId, Long id, PlanDTO.UpdatePlanRequest request) {
        LambdaQueryWrapper<Plan> query = new LambdaQueryWrapper<>();
        query.eq(Plan::getId, id).eq(Plan::getUserId, userId);
        Plan plan = planMapper.selectOne(query);
        if (plan == null) {
            throw new RuntimeException("规划不存在");
        }

        // 更新基本信息
        if (request.getWeeklyReset() != null) {
            plan.setWeeklyReset(request.getWeeklyReset());
            plan.setUpdatedAt(LocalDateTime.now());
            planMapper.updateById(plan);
        }

        // 更新每日数据
        if (request.getDays() != null) {
            for (Map.Entry<String, PlanDTO.DayPlan> entry : request.getDays().entrySet()) {
                LocalDate date = LocalDate.parse(entry.getKey());
                PlanDTO.DayPlan dayPlan = entry.getValue();

                LambdaQueryWrapper<PlanDay> dayQuery = new LambdaQueryWrapper<>();
                dayQuery.eq(PlanDay::getPlanId, id).eq(PlanDay::getDate, date);
                PlanDay planDay = planDayMapper.selectOne(dayQuery);
                if (planDay != null) {
                    // 获取原有菜品列表
                    List<Long> oldBreakfast = JSONUtil.toList(planDay.getBreakfast(), Long.class);
                    List<Long> oldLunch = JSONUtil.toList(planDay.getLunch(), Long.class);
                    List<Long> oldDinner = JSONUtil.toList(planDay.getDinner(), Long.class);

                    List<Long> newBreakfast = dayPlan.getBreakfast() != null ? dayPlan.getBreakfast() : new ArrayList<>();
                    List<Long> newLunch = dayPlan.getLunch() != null ? dayPlan.getLunch() : new ArrayList<>();
                    List<Long> newDinner = dayPlan.getDinner() != null ? dayPlan.getDinner() : new ArrayList<>();

                    // 计算每个菜品的数量变化（支持重复添加/移除）
                    Map<Long, Long> toConsume = new HashMap<>();  // 需要扣减的
                    Map<Long, Long> toReturn = new HashMap<>();   // 需要退回的
                    countDishDiff(oldBreakfast, newBreakfast, toConsume, toReturn);
                    countDishDiff(oldLunch, newLunch, toConsume, toReturn);
                    countDishDiff(oldDinner, newDinner, toConsume, toReturn);

                    // 从冰箱扣减
                    for (Map.Entry<Long, Long> e : toConsume.entrySet()) {
                        Long dishId = e.getKey();
                        int count = e.getValue().intValue();
                        for (int i = 0; i < count; i++) {
                            if (!fridgeService.consumeDish(userId, dishId, 1)) {
                                throw new RuntimeException("冰箱中菜品库存不足: " + dishId);
                            }
                        }
                    }

                    // 退回到冰箱
                    for (Map.Entry<Long, Long> e : toReturn.entrySet()) {
                        Long dishId = e.getKey();
                        int count = e.getValue().intValue();
                        FridgeDTO.AddRequest returnRequest = new FridgeDTO.AddRequest();
                        returnRequest.setDishId(dishId);
                        returnRequest.setQuantity(count);
                        fridgeService.addToFridge(userId, returnRequest);
                    }

                    planDay.setBreakfast(JSONUtil.toJsonStr(newBreakfast));
                    planDay.setLunch(JSONUtil.toJsonStr(newLunch));
                    planDay.setDinner(JSONUtil.toJsonStr(newDinner));
                    planDayMapper.updateById(planDay);
                }
            }
        }

        // 添加或删除菜品
        if (StrUtil.isNotBlank(request.getAction())) {
            handleDishAction(id, request);
        }

        return toResponse(plan);
    }

    private void handleDishAction(Long planId, PlanDTO.UpdatePlanRequest request) {
        LocalDate date = LocalDate.parse(request.getDate());
        String meal = request.getMeal();
        Long dishId = request.getDishId();
        Long userId = request.getUserId(); // 需要用户ID来检查冰箱

        LambdaQueryWrapper<PlanDay> dayQuery = new LambdaQueryWrapper<>();
        dayQuery.eq(PlanDay::getPlanId, planId).eq(PlanDay::getDate, date);
        PlanDay planDay = planDayMapper.selectOne(dayQuery);
        if (planDay == null) {
            throw new RuntimeException("日期不在规划范围内");
        }

        String json = switch (meal) {
            case "breakfast" -> planDay.getBreakfast();
            case "lunch" -> planDay.getLunch();
            case "dinner" -> planDay.getDinner();
            default -> throw new RuntimeException("无效的餐次");
        };

        List<Long> dishIds = JSONUtil.toList(json, Long.class);
        if ("addDish".equals(request.getAction())) {
            // 从冰箱扣减库存
            if (userId != null) {
                boolean success = fridgeService.consumeDish(userId, dishId, 1);
                if (!success) {
                    throw new RuntimeException("冰箱中该菜品库存不足");
                }
            }
            dishIds.add(dishId);
        } else if ("removeDish".equals(request.getAction())) {
            if (dishIds.contains(dishId)) {
                dishIds.remove(dishId);  // 只移除一个
                // 移除时可以退回冰箱（可选功能）
                if (userId != null && Boolean.TRUE.equals(request.getReturnToFridge())) {
                    FridgeDTO.AddRequest returnRequest = new FridgeDTO.AddRequest();
                    returnRequest.setDishId(dishId);
                    returnRequest.setQuantity(1);
                    fridgeService.addToFridge(userId, returnRequest);
                }
            }
        }

        String updatedJson = JSONUtil.toJsonStr(dishIds);
        switch (meal) {
            case "breakfast" -> planDay.setBreakfast(updatedJson);
            case "lunch" -> planDay.setLunch(updatedJson);
            case "dinner" -> planDay.setDinner(updatedJson);
        }
        planDayMapper.updateById(planDay);
    }

    @Transactional
    public void deletePlan(Long userId, Long id) {
        LambdaQueryWrapper<Plan> query = new LambdaQueryWrapper<>();
        query.eq(Plan::getId, id).eq(Plan::getUserId, userId);
        Plan plan = planMapper.selectOne(query);
        if (plan == null) {
            throw new RuntimeException("规划不存在");
        }

        // 删除每日数据
        LambdaQueryWrapper<PlanDay> dayQuery = new LambdaQueryWrapper<>();
        dayQuery.eq(PlanDay::getPlanId, id);
        planDayMapper.delete(dayQuery);

        // 删除规划
        planMapper.deleteById(id);
    }

    public PlanDTO.ConflictResponse checkConflict(Long userId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        List<Plan> conflicts = findConflicts(userId, startDate, endDate, excludeId);

        PlanDTO.ConflictResponse response = new PlanDTO.ConflictResponse();
        response.setHasConflict(!conflicts.isEmpty());
        response.setConflicts(conflicts.stream().map(p -> {
            PlanDTO.ConflictPlan cp = new PlanDTO.ConflictPlan();
            cp.setId(p.getId());
            cp.setStartDate(p.getStartDate());
            cp.setEndDate(p.getEndDate());
            return cp;
        }).collect(Collectors.toList()));
        return response;
    }

    public PlanDTO.PlannedDatesResponse getPlannedDates(Long userId, Long excludeId) {
        LambdaQueryWrapper<Plan> query = new LambdaQueryWrapper<>();
        query.eq(Plan::getUserId, userId);
        if (excludeId != null) {
            query.ne(Plan::getId, excludeId);
        }
        List<Plan> plans = planMapper.selectList(query);

        Set<String> dates = new HashSet<>();
        for (Plan plan : plans) {
            long days = ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1;
            for (int i = 0; i < days; i++) {
                dates.add(plan.getStartDate().plusDays(i).toString());
            }
        }

        PlanDTO.PlannedDatesResponse response = new PlanDTO.PlannedDatesResponse();
        response.setDates(new ArrayList<>(dates));
        return response;
    }

    public PlanDTO.NutritionResponse calculateNutrition(List<Long> dishIds) {
        PlanDTO.NutritionResponse response = new PlanDTO.NutritionResponse();
        response.setCalories(0);
        response.setProtein(0);
        response.setCarbs(0);
        response.setFat(0);

        if (CollUtil.isEmpty(dishIds)) {
            return response;
        }

        List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
        for (Dish dish : dishes) {
            response.setCalories(response.getCalories() + (dish.getCalories() != null ? dish.getCalories() : 0));
            response.setProtein(response.getProtein() + (dish.getProtein() != null ? dish.getProtein() : 0));
            response.setCarbs(response.getCarbs() + (dish.getCarbs() != null ? dish.getCarbs() : 0));
            response.setFat(response.getFat() + (dish.getFat() != null ? dish.getFat() : 0));
        }
        return response;
    }

    private List<Plan> findConflicts(Long userId, LocalDate startDate, LocalDate endDate, Long excludeId) {
        LambdaQueryWrapper<Plan> query = new LambdaQueryWrapper<>();
        query.eq(Plan::getUserId, userId)
                .le(Plan::getStartDate, endDate)
                .ge(Plan::getEndDate, startDate);
        if (excludeId != null) {
            query.ne(Plan::getId, excludeId);
        }
        return planMapper.selectList(query);
    }

    private PlanDTO.PlanListItem toListItem(Plan plan) {
        PlanDTO.PlanListItem item = new PlanDTO.PlanListItem();
        item.setId(plan.getId());
        item.setStartDate(plan.getStartDate());
        item.setEndDate(plan.getEndDate());
        item.setWeeklyReset(plan.getWeeklyReset());
        item.setCreatedAt(plan.getCreatedAt() != null ?
                plan.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null);
        return item;
    }

    private PlanDTO.PlanResponse toResponse(Plan plan) {
        PlanDTO.PlanResponse response = new PlanDTO.PlanResponse();
        response.setId(plan.getId());
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setWeeklyReset(plan.getWeeklyReset());
        response.setCreatedAt(plan.getCreatedAt() != null ?
                plan.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null);

        // 查询每日数据
        LambdaQueryWrapper<PlanDay> dayQuery = new LambdaQueryWrapper<>();
        dayQuery.eq(PlanDay::getPlanId, plan.getId()).orderByAsc(PlanDay::getDate);
        List<PlanDay> planDays = planDayMapper.selectList(dayQuery);

        Map<String, PlanDTO.DayPlan> days = new LinkedHashMap<>();
        for (PlanDay pd : planDays) {
            PlanDTO.DayPlan dayPlan = new PlanDTO.DayPlan();
            dayPlan.setBreakfast(JSONUtil.toList(pd.getBreakfast(), Long.class));
            dayPlan.setLunch(JSONUtil.toList(pd.getLunch(), Long.class));
            dayPlan.setDinner(JSONUtil.toList(pd.getDinner(), Long.class));
            days.put(pd.getDate().toString(), dayPlan);
        }
        response.setDays(days);
        return response;
    }

    /**
     * 计算菜品数量差异（新列表比旧列表多出/少多少个）
     * @param toConsume 需要扣减的菜品及数量
     * @param toReturn 需要退回的菜品及数量
     */
    private void countDishDiff(List<Long> oldList, List<Long> newList, Map<Long, Long> toConsume, Map<Long, Long> toReturn) {
        Map<Long, Long> oldCount = new HashMap<>();
        Map<Long, Long> newCount = new HashMap<>();

        for (Long id : oldList) {
            oldCount.merge(id, 1L, Long::sum);
        }
        for (Long id : newList) {
            newCount.merge(id, 1L, Long::sum);
        }

        // 所有涉及的菜品ID
        Set<Long> allIds = new HashSet<>();
        allIds.addAll(oldCount.keySet());
        allIds.addAll(newCount.keySet());

        for (Long dishId : allIds) {
            long oldTotal = oldCount.getOrDefault(dishId, 0L);
            long newTotal = newCount.getOrDefault(dishId, 0L);
            long diff = newTotal - oldTotal;
            if (diff > 0) {
                // 新增多，需要扣减
                toConsume.merge(dishId, diff, Long::sum);
            } else if (diff < 0) {
                // 减少了，需要退回
                toReturn.merge(dishId, -diff, Long::sum);
            }
        }
    }
}