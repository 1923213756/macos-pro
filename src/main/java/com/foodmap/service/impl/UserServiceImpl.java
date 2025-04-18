package com.foodmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodmap.exception.BadRequestException;
import com.foodmap.exception.NotFoundException;
import com.foodmap.exception.UnauthorizedException;
import com.foodmap.mapper.UserMapper;
import com.foodmap.entity.pojo.User;
import com.foodmap.service.UserService;
import com.foodmap.util.RedisCacheUtil;
import com.foodmap.util.SecurityUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final RedisCacheUtil cacheUtil;

    // 修改构造函数，添加RedisCacheUtil
    @Autowired
    public UserServiceImpl(UserMapper userMapper, RedisCacheUtil cacheUtil) {
        this.userMapper = userMapper;
        this.cacheUtil = cacheUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(User user) {
        // 主动查询防重复
        if (userMapper.countByUsername(user.getUserName()) > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (userMapper.countByPhone(user.getPhone()) > 0) {
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
        // 尝试从缓存获取用户信息
        String cacheKey = "users:name:" + username;
        Object cachedData = cacheUtil.get(cacheKey);
        User user;

        if (cachedData != null) {
            user = (User) cachedData;
            log.info("缓存命中: 用户 {}", username);
        } else {
            // 缓存未命中，从数据库查询
            user = userMapper.selectByUsername(username);
            if (user == null) throw new IllegalArgumentException("用户不存在");

            // 写入缓存
            cacheUtil.set(cacheKey, user);
            log.info("数据库查询并缓存: 用户 {}", user.getUserName());
        }

        // 验证密码 - 保持原有逻辑
        if (!SecurityUtil.checkPassword(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        return user;
    }

    @Override
    public User getUserByName(String username) {
        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // 构建缓存key
        String cacheKey = "users:name:" + username;

        // 尝试从缓存获取
        Object cachedData = cacheUtil.get(cacheKey);
        if (cachedData != null) {
            log.info("从缓存获取用户信息: {}", username);
            return (User) cachedData;
        }

        // 从数据库查询用户信息
        User user = userMapper.selectByUsername(username);

        // 用户不存在时的处理
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 存入缓存
        cacheUtil.set(cacheKey, user);
        log.info("数据库获取用户信息并缓存: {}", username);

        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserPassword(Long userId, String oldPassword, String newPassword) {
        // 参数验证 - 保持原有逻辑
        if (userId == null || userId <= 0) {
            throw new BadRequestException("无效的用户ID");
        }

        if (!StringUtils.hasText(oldPassword)) {
            throw new BadRequestException("当前密码不能为空");
        }

        if (!StringUtils.hasText(newPassword)) {
            throw new BadRequestException("新密码不能为空");
        }

        if (oldPassword.equals(newPassword)) {
            throw new BadRequestException("新密码不能与当前密码相同");
        }

        // 获取用户信息 - 使用MyBatis Plus的getById方法
        User user = this.getById(userId);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }

        // 验证旧密码
        if (!SecurityUtil.checkPassword(oldPassword, user.getPassword())) {
            throw new UnauthorizedException("当前密码不正确");
        }

        // 密码格式验证
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            throw new BadRequestException("新密码长度必须在8-20个字符之间");
        }

        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$")) {
            throw new BadRequestException("新密码至少需包含一个大写字母、一个小写字母和一个数字");
        }

        // 加密新密码
        String encryptedNewPassword = SecurityUtil.encryptPassword(newPassword);

        // 使用原有的更新方式
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("userId", userId)
                .set(encryptedNewPassword!=null,"password", encryptedNewPassword)
                .set("updateTime", LocalDateTime.now());

        // 执行更新操作
        boolean updated = this.update(updateWrapper);
        if (!updated) {
            throw new BadRequestException("密码更新失败");
        }

        // 清除相关缓存
        cacheUtil.delete("users:id:" + userId);
        cacheUtil.delete("users:name:" + user.getUserName());

        log.info("用户 {} ({}) 密码修改成功", userId, user.getUserName());
    }
}