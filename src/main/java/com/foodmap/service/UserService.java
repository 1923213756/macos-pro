package com.foodmap.service;

import com.foodmap.entity.pojo.User;

import java.util.Optional;

public interface UserService {
    // 注册用户
    void register(User user);

    // 登录校验
    User login(String username, String rawPassword);

    User getUserByName(String username);

    void updateUserPassword(Long userId, String oldPassword, String newPassword);
    /**
     * 获取当前登录用户
     * 如果用户未登录，将抛出异常
     *
     * @return 当前登录的用户实体
     */
    User getCurrentUser();

    /**
     * 获取当前登录用户（Optional包装）
     * 如果用户未登录，返回空Optional
     *
     * @return 包含当前用户的Optional或空Optional
     */
    Optional<User> getCurrentUserOptional();
}