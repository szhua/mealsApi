package com.sancanji.mealsapi.controller.admin;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sancanji.mealsapi.entity.*;
import com.sancanji.mealsapi.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final DishMapper dishMapper;
    private final PlanMapper planMapper;
    private final PlanDayMapper planDayMapper;
    private final CheckInMapper checkInMapper;
    private final FridgeMapper fridgeMapper;

    @GetMapping
    public String index() {
        return "redirect:/admin/users";
    }

    @GetMapping("/users")
    public String users(Model model,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "20") int pageSize) {
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.orderByDesc(User::getCreatedAt);
        Page<User> userPage = userMapper.selectPage(new Page<>(page, pageSize), query);

        // 统计每个用户的数据
        Map<Long, UserStats> statsMap = new HashMap<>();
        for (User user : userPage.getRecords()) {
            UserStats stats = new UserStats();

            LambdaQueryWrapper<Dish> dishQuery = new LambdaQueryWrapper<>();
            dishQuery.eq(Dish::getUserId, user.getId());
            stats.setDishCount(dishMapper.selectCount(dishQuery));

            LambdaQueryWrapper<Plan> planQuery = new LambdaQueryWrapper<>();
            planQuery.eq(Plan::getUserId, user.getId());
            stats.setPlanCount(planMapper.selectCount(planQuery));

            LambdaQueryWrapper<CheckIn> checkInQuery = new LambdaQueryWrapper<>();
            checkInQuery.eq(CheckIn::getUserId, user.getId());
            stats.setCheckInCount(checkInMapper.selectCount(checkInQuery));

            LambdaQueryWrapper<Fridge> fridgeQuery = new LambdaQueryWrapper<>();
            fridgeQuery.eq(Fridge::getUserId, user.getId());
            stats.setFridgeCount(fridgeMapper.selectCount(fridgeQuery));

            statsMap.put(user.getId(), stats);
        }

        model.addAttribute("users", userPage.getRecords());
        model.addAttribute("total", userPage.getTotal());
        model.addAttribute("page", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("totalPages", (int) Math.ceil((double) userPage.getTotal() / pageSize));
        model.addAttribute("statsMap", statsMap);
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return "redirect:/admin/users";
        }

        // 用户菜品
        LambdaQueryWrapper<Dish> dishQuery = new LambdaQueryWrapper<>();
        dishQuery.eq(Dish::getUserId, id).orderByDesc(Dish::getCreatedAt);
        List<Dish> dishes = dishMapper.selectList(dishQuery);

        // 用户规划
        LambdaQueryWrapper<Plan> planQuery = new LambdaQueryWrapper<>();
        planQuery.eq(Plan::getUserId, id).orderByDesc(Plan::getCreatedAt);
        List<Plan> plans = planMapper.selectList(planQuery);

        // 打卡记录
        LambdaQueryWrapper<CheckIn> checkInQuery = new LambdaQueryWrapper<>();
        checkInQuery.eq(CheckIn::getUserId, id).orderByDesc(CheckIn::getCheckDate).last("LIMIT 30");
        List<CheckIn> checkIns = checkInMapper.selectList(checkInQuery);

        // 冰箱库存
        LambdaQueryWrapper<Fridge> fridgeQuery = new LambdaQueryWrapper<>();
        fridgeQuery.eq(Fridge::getUserId, id).orderByDesc(Fridge::getUpdatedAt);
        List<Fridge> fridgeItems = fridgeMapper.selectList(fridgeQuery);

        // 获取菜品详情
        Map<Long, Dish> fridgeDishMap = new HashMap<>();
        if (!fridgeItems.isEmpty()) {
            List<Long> dishIds = fridgeItems.stream().map(Fridge::getDishId).collect(Collectors.toList());
            List<Dish> fridgeDishes = dishMapper.selectBatchIds(dishIds);
            fridgeDishMap = fridgeDishes.stream().collect(Collectors.toMap(Dish::getId, d -> d));
        }

        List<FridgeItemView> fridgeView = new ArrayList<>();
        for (Fridge item : fridgeItems) {
            FridgeItemView view = new FridgeItemView();
            view.setId(item.getId());
            view.setDishId(item.getDishId());
            view.setQuantity(item.getQuantity());
            view.setUnit(item.getUnit());
            view.setExpiryDate(item.getExpiryDate());
            Dish dish = fridgeDishMap.get(item.getDishId());
            if (dish != null) {
                view.setDishName(dish.getName());
                view.setDishImage(dish.getImage());
            }
            fridgeView.add(view);
        }

        model.addAttribute("user", user);
        model.addAttribute("dishes", dishes);
        model.addAttribute("plans", plans);
        model.addAttribute("checkIns", checkIns);
        model.addAttribute("fridgeItems", fridgeView);
        return "admin/user-detail";
    }

    @GetMapping("/plans/{planId}")
    public String planDetail(@PathVariable Long planId, Model model) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            return "redirect:/admin/users";
        }

        User user = userMapper.selectById(plan.getUserId());

        // 查询规划的所有日期
        LambdaQueryWrapper<PlanDay> dayQuery = new LambdaQueryWrapper<>();
        dayQuery.eq(PlanDay::getPlanId, planId).orderByAsc(PlanDay::getDate);
        List<PlanDay> planDays = planDayMapper.selectList(dayQuery);

        // 收集所有菜品ID
        Set<Long> dishIds = new HashSet<>();
        for (PlanDay day : planDays) {
            dishIds.addAll(parseDishIds(day.getBreakfast()));
            dishIds.addAll(parseDishIds(day.getLunch()));
            dishIds.addAll(parseDishIds(day.getDinner()));
        }

        // 查询所有菜品
        Map<Long, Dish> dishMap = new HashMap<>();
        if (!dishIds.isEmpty()) {
            List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
            dishMap = dishes.stream().collect(Collectors.toMap(Dish::getId, d -> d));
        }

        // 计算每天的统计
        List<DaySummary> daySummaries = new ArrayList<>();
        for (PlanDay day : planDays) {
            DaySummary summary = new DaySummary();
            summary.setDate(day.getDate());
            summary.setBreakfastDishes(getDishNames(day.getBreakfast(), dishMap));
            summary.setLunchDishes(getDishNames(day.getLunch(), dishMap));
            summary.setDinnerDishes(getDishNames(day.getDinner(), dishMap));

            // 计算营养
            int calories = 0, protein = 0, carbs = 0, fat = 0;
            for (Long id : parseDishIds(day.getBreakfast())) {
                Dish d = dishMap.get(id);
                if (d != null) {
                    calories += d.getCalories() != null ? d.getCalories() : 0;
                    protein += d.getProtein() != null ? d.getProtein() : 0;
                    carbs += d.getCarbs() != null ? d.getCarbs() : 0;
                    fat += d.getFat() != null ? d.getFat() : 0;
                }
            }
            for (Long id : parseDishIds(day.getLunch())) {
                Dish d = dishMap.get(id);
                if (d != null) {
                    calories += d.getCalories() != null ? d.getCalories() : 0;
                    protein += d.getProtein() != null ? d.getProtein() : 0;
                    carbs += d.getCarbs() != null ? d.getCarbs() : 0;
                    fat += d.getFat() != null ? d.getFat() : 0;
                }
            }
            for (Long id : parseDishIds(day.getDinner())) {
                Dish d = dishMap.get(id);
                if (d != null) {
                    calories += d.getCalories() != null ? d.getCalories() : 0;
                    protein += d.getProtein() != null ? d.getProtein() : 0;
                    carbs += d.getCarbs() != null ? d.getCarbs() : 0;
                    fat += d.getFat() != null ? d.getFat() : 0;
                }
            }
            summary.setCalories(calories);
            summary.setProtein(protein);
            summary.setCarbs(carbs);
            summary.setFat(fat);

            daySummaries.add(summary);
        }

        model.addAttribute("plan", plan);
        model.addAttribute("user", user);
        model.addAttribute("daySummaries", daySummaries);
        return "admin/plan-detail";
    }

    private List<Long> parseDishIds(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return JSONUtil.toList(json, Long.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<String> getDishNames(String json, Map<Long, Dish> dishMap) {
        List<Long> ids = parseDishIds(json);
        List<String> names = new ArrayList<>();
        for (Long id : ids) {
            Dish dish = dishMap.get(id);
            if (dish != null) {
                names.add(dish.getName());
            }
        }
        return names;
    }

    public static class DaySummary {
        private LocalDate date;
        private List<String> breakfastDishes;
        private List<String> lunchDishes;
        private List<String> dinnerDishes;
        private int calories;
        private int protein;
        private int carbs;
        private int fat;

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public List<String> getBreakfastDishes() { return breakfastDishes; }
        public void setBreakfastDishes(List<String> breakfastDishes) { this.breakfastDishes = breakfastDishes; }
        public List<String> getLunchDishes() { return lunchDishes; }
        public void setLunchDishes(List<String> lunchDishes) { this.lunchDishes = lunchDishes; }
        public List<String> getDinnerDishes() { return dinnerDishes; }
        public void setDinnerDishes(List<String> dinnerDishes) { this.dinnerDishes = dinnerDishes; }
        public int getCalories() { return calories; }
        public void setCalories(int calories) { this.calories = calories; }
        public int getProtein() { return protein; }
        public void setProtein(int protein) { this.protein = protein; }
        public int getCarbs() { return carbs; }
        public void setCarbs(int carbs) { this.carbs = carbs; }
        public int getFat() { return fat; }
        public void setFat(int fat) { this.fat = fat; }
    }

    public static class UserStats {
        private long dishCount;
        private long planCount;
        private long checkInCount;
        private long fridgeCount;

        public long getDishCount() { return dishCount; }
        public void setDishCount(long dishCount) { this.dishCount = dishCount; }
        public long getPlanCount() { return planCount; }
        public void setPlanCount(long planCount) { this.planCount = planCount; }
        public long getCheckInCount() { return checkInCount; }
        public void setCheckInCount(long checkInCount) { this.checkInCount = checkInCount; }
        public long getFridgeCount() { return fridgeCount; }
        public void setFridgeCount(long fridgeCount) { this.fridgeCount = fridgeCount; }
    }

    public static class FridgeItemView {
        private Long id;
        private Long dishId;
        private String dishName;
        private String dishImage;
        private Integer quantity;
        private String unit;
        private LocalDate expiryDate;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getDishId() { return dishId; }
        public void setDishId(Long dishId) { this.dishId = dishId; }
        public String getDishName() { return dishName; }
        public void setDishName(String dishName) { this.dishName = dishName; }
        public String getDishImage() { return dishImage; }
        public void setDishImage(String dishImage) { this.dishImage = dishImage; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    }
}