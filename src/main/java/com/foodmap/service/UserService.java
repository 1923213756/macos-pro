package com.foodmap.service;

import com.foodmap.entity.pojo.User;

public interface UserService {
    // 注册用户
    void register(User user);

    // 登录校验
    User login(String username, String rawPassword);

    User getUserByName(String username);

    void updateUserPassword(Long userId, String oldPassword, String newPassword);
}