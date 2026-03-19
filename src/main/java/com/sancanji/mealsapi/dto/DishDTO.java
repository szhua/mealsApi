package com.sancanji.mealsapi.dto;

import lombok.Data;
import java.util.List;

public class DishDTO {

    @Data
    public static class DishRequest {
        private String name;
        private String image;
        private Long categoryId;
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fat;
        private List<String> mealTypes;
    }

    @Data
    public static class DishResponse {
        private Long id;
        private String name;
        private String image;
        private Long categoryId;
        private String categoryName;
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fat;
        private List<String> mealTypes;
        private Long createdAt;
    }

    @Data
    public static class BatchRequest {
        private List<Long> ids;
    }

    @Data
    public static class InPlanResponse {
        private Boolean inPlan;
        private String planDateRange;
    }

    @Data
    public static class NutritionResponse {
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fat;
    }
}