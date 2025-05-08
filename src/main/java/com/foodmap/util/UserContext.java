package com.foodmap.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContext {

    /**
     * 获取当前登录用户ID
     * @return 用户ID，如果未登录则可能返回null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 根据具体实现，这里可能是从Principal中获取，或者从Authentication中的其他属性获取
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

}