package com.foodmap.entity.dto;

import lombok.Data;

@Data
public class ShopAuthInfo {
    private Long shopId;
    private String shopName;
    private String password;
}