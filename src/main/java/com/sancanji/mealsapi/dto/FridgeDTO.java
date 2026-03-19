package com.sancanji.mealsapi.dto;

import lombok.Data;

import java.time.LocalDate;

public class FridgeDTO {

    @Data
    public static class FridgeItem {
        private Long id;
        private Long dishId;
        private String dishName;
        private String dishImage;
        private Long categoryId;
        private Integer quantity;
        private String unit;
        private LocalDate expiryDate;
    }

    @Data
    public static class AddRequest {
        private Long dishId;
        private Integer quantity;
        private String unit;
        private LocalDate expiryDate;
    }

    @Data
    public static class UpdateRequest {
        private Integer quantity;
        private LocalDate expiryDate;
    }
}