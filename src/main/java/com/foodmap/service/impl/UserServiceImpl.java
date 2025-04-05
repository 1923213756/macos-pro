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
        if (userMapper.countByPhone(user.getPhone())>0) {
            throw new IllegalArgumentException("手机号已注册");
        }

        // 密码加密
        String encodedPwd = SecurityUtil.encryptPassword(user.getPassword());
        user.setPassword(encodedPwd);

        // 插入用户
        userMapper.insertUser(user);
    }

    @Override
    public User login(String username, String rawPassword) {
        User users = userMapper.selectByUsername(username);
        if (users == null) throw new IllegalArgumentException("用户不存在");

        // 改用 BCrypt 验证
        if (!SecurityUtil.checkPassword(rawPassword, users.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        return users;
    }

    @Override
    public User getUserByName(String username) {
        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 从数据库查询用户信息
        User user = userMapper.selectByUsername(username);

        // 用户不存在时的处理
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        return user;
    }


}