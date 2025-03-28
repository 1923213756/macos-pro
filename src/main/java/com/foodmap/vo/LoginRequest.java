package com.foodmap.vo;

import lombok.Data;

/**
 * 商铺登录请求参数
 */
@Data
public class LoginRequest {
    /**
     * 商铺名称
     */
    private String shopName;

    /**
     * 密码
     */
    private String password;
}