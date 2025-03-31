package com.foodmap.service.impl;

import com.foodmap.dao.UserMapper;
import com.foodmap.entity.User;
import com.foodmap.service.UserService;
import com.foodmap.util.SecurityUtil;

import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    //传入方法
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }


    @Override
    public void register(User user) {
        // 主动查询防重复
        if (userMapper.countByUsername(user.getUserName()) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.countByPhone(user.getPhone()) > 0) {
            throw new IllegalArgumentException("手机号已注册");
        }

        // 密码加密
        String encodedPwd = SecurityUtil.encryptPassword(user.getUserPassword());
        user.setUserPassword(encodedPwd);

        // 插入用户
        userMapper.insertUser(user);
    }

    @Override
    public User login(String username, String rawPassword) {
        User users = userMapper.selectByUsername(username);
        if (users == null) throw new IllegalArgumentException("用户不存在");

        // 改用 BCrypt 验证
        if (!SecurityUtil.checkPassword(rawPassword, users.getUserPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        return users;
    }



}