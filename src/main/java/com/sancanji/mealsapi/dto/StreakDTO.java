package com.sancanji.mealsapi.dto;

import lombok.Data;
import java.util.List;

public class StreakDTO {

    @Data
    public static class StreakResponse {
        private Integer current;
        private Integer longest;
        private String lastDate;
        private List<String> checkIns;
    }

    @Data
    public static class CheckInRequest {
        private String date;
    }
}