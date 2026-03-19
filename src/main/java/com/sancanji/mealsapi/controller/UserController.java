package com.sancanji.mealsapi.controller;

import com.sancanji.mealsapi.dto.ApiResponse;
import com.sancanji.mealsapi.dto.UserDTO;
import com.sancanji.mealsapi.service.UserService;
import com.sancanji.mealsapi.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 微信登录
     */
    @PostMapping("/login")
    public ApiResponse<UserDTO.LoginResponse> login(@RequestBody UserDTO.LoginRequest request) {
        return ApiResponse.success("登录成功", userService.login(request.getCode()));
    }

    /**
     * 获取手机号
     */
    @PostMapping("/phone")
    public ApiResponse<UserDTO.UserInfo> updatePhone(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UserDTO.PhoneRequest request) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success("绑定成功", userService.updatePhone(userId, request.getCode()));
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    public ApiResponse<UserDTO.UserInfo> getUserInfo(@RequestHeader("Authorization") String authorization) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success(userService.getUserInfo(userId));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/info")
    public ApiResponse<UserDTO.UserInfo> updateUserInfo(
            @RequestHeader("Authorization") String authorization,
            @RequestBody UserDTO.UserInfo userInfo) {
        Long userId = getUserIdFromToken(authorization);
        return ApiResponse.success("更新成功", userService.updateUserInfo(userId, userInfo));
    }

    private Long getUserIdFromToken(String authorization) {
        String token = authorization.replace("Bearer ", "");
        return jwtUtil.getUserId(token);
    }
}