package com.foodmap.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SecurityUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    // 密码加密
    public static String encryptPassword(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // 密码验证
    public static boolean checkPassword(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}