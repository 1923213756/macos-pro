package com.foodmap.entity;


import lombok.Data;

@Data
public class ShopQueryEntity {
    private String name;
    private Long categoryId;
    private String areaCode;
    private Integer sortType; // 1:评分排序 2:距离排序 ...
    private Integer page = 1;
    private Integer pageSize = 10;
}