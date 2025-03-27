package com.foodmap.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Shop {
    private Long shopId;
    private String shopName;
    private String password;
    private String address;
    private String contactTel;
    private String businessHours;
    private BigDecimal compositeScore = BigDecimal.ZERO; // 默认值0.0


    // 添加商铺分类 (甜品、火锅、料理等)
    private String category;    // 商铺分类：甜品、火锅、料理等

    // 添加地区 (白云区、海珠区等)
    private String district;    // 地区：白云区、海珠区等

    private String description; // 商铺描述

    // 创建时间和更新时间 - 由系统内置逻辑自动生成和维护
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Integer status;     // 商铺状态：0-关闭，1-营业中
}