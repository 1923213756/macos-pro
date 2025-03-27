package com.foodmap.vo;

import lombok.Data;

@Data
public class LoginRequest {
    private String shopName;
    private String password;
}