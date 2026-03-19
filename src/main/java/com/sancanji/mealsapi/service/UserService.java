package com.sancanji.mealsapi.service;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sancanji.mealsapi.dto.UserDTO;
import com.sancanji.mealsapi.entity.User;
import com.sancanji.mealsapi.mapper.UserMapper;
import com.sancanji.mealsapi.util.JwtUtil;
import com.sancanji.mealsapi.util.WeChatUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final WeChatUtil weChatUtil;

    @Transactional
    public UserDTO.LoginResponse login(String code) {
        // 调用微信接口获取openid
        JSONObject session = weChatUtil.code2Session(code);
        String openId = session.getStr("openid");

        if (openId == null) {
            throw new RuntimeException("微信登录失败: " + session.getStr("errmsg"));
        }

        // 查找或创建用户
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getOpenId, openId);
        User user = userMapper.selectOne(query);

        if (user == null) {
            user = new User();
            user.setOpenId(openId);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        // 生成token
        String token = jwtUtil.generateToken(user.getId(), openId);

        // 返回结果
        UserDTO.LoginResponse response = new UserDTO.LoginResponse();
        response.setToken(token);
        response.setUserInfo(toUserInfo(user));
        return response;
    }

    @Transactional
    public UserDTO.UserInfo updatePhone(Long userId, String code) {
        // 获取手机号
        JSONObject result = weChatUtil.getPhoneNumber(code);
        JSONObject phoneInfo = result.getJSONObject("phone_info");
        if (phoneInfo == null) {
            throw new RuntimeException("获取手机号失败");
        }
        String phone = phoneInfo.getStr("phoneNumber");

        // 更新用户手机号
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPhone(phone);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return toUserInfo(user);
    }

    public UserDTO.UserInfo getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return toUserInfo(user);
    }

    @Transactional
    public UserDTO.UserInfo updateUserInfo(Long userId, UserDTO.UserInfo userInfo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (userInfo.getNickname() != null) {
            user.setNickname(userInfo.getNickname());
        }
        if (userInfo.getAvatar() != null) {
            user.setAvatar(userInfo.getAvatar());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toUserInfo(user);
    }

    private UserDTO.UserInfo toUserInfo(User user) {
        UserDTO.UserInfo info = new UserDTO.UserInfo();
        info.setId(user.getId());
        info.setOpenId(user.getOpenId());
        info.setPhone(user.getPhone());
        info.setNickname(user.getNickname());
        info.setAvatar(user.getAvatar());
        return info;
    }
}