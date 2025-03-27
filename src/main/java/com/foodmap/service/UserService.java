package com.foodmap.service;

import com.foodmap.entity.User;

public interface UserService {
    // 注册用户
    void register(User user);

    // 登录校验
    User login(String username, String rawPassword);
}