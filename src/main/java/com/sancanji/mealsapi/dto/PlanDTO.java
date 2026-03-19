package com.sancanji.mealsapi.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class PlanDTO {

    @Data
    public static class PlanRequest {
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean weeklyReset;
    }

    @Data
    public static class UpdatePlanRequest {
        private Boolean weeklyReset;
        private Map<String, DayPlan> days;
        private String action;
        private String date;
        private String meal;
        private Long dishId;
    }

    @Data
    public static class DayPlan {
        private List<Long> breakfast;
        private List<Long> lunch;
        private List<Long> dinner;
    }

    @Data
    public static class PlanResponse {
        private Long id;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean weeklyReset;
        private Map<String, DayPlan> days;
        private Long createdAt;
    }

    @Data
    public static class PlanListItem {
        private Long id;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean weeklyReset;
        private Long createdAt;
    }

    @Data
    public static class ConflictResponse {
        private Boolean hasConflict;
        private List<ConflictPlan> conflicts;
    }

    @Data
    public static class ConflictPlan {
        private Long id;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Data
    public static class PlannedDatesResponse {
        private List<String> dates;
    }

    @Data
    public static class CalculateNutritionRequest {
        private List<Long> dishIds;
    }

    @Data
    public static class NutritionResponse {
        private Integer calories;
        private Integer protein;
        private Integer carbs;
        private Integer fat;
    }
}