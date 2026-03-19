package com.sancanji.mealsapi.dto;

import lombok.Data;

public class UserDTO {

    @Data
    public static class LoginRequest {
        private String code;
    }

    @Data
    public static class PhoneRequest {
        private String code;
    }

    @Data
    public static class LoginResponse {
        private String token;
        private UserInfo userInfo;
    }

    @Data
    public static class UserInfo {
        private Long id;
        private String openId;
        private String phone;
        private String nickname;
        private String avatar;
    }
}