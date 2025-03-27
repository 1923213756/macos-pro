package com.foodmap.entity;

import lombok.Data;

@Data
public class ShopAuthInfo {
    private Long shopId;
    private String shopName;
    private String password;
}