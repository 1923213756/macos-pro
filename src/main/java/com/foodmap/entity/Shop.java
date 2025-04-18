package com.foodmap.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "商铺实体")
public class Shop {
    @Schema(description = "商铺ID", example = "1001")
    private Long shopId;

    @Schema(description = "商铺名称", example = "美味小厨")
    private String shopName;

    @Schema(description = "商铺密码", example = "password123",
            hidden = true) // 密码字段在API文档中隐藏
    private String password;

    @Schema(description = "商铺地址", example = "广州市天河区天河路385号")
    private String address;

    @Schema(description = "联系电话", example = "020-12345678")
    private String contactTel;

    @Schema(description = "营业时间", example = "周一至周日 09:00-22:00")
    private String businessHours;

    @Schema(description = "综合评分", example = "4.8")
    private BigDecimal compositeScore; // 默认值0.0

    @Schema(description = "商铺分类", example = "中餐",
            allowableValues = {"中餐", "西餐", "火锅", "烧烤", "甜品", "快餐"})
    private String category;    // 商铺分类：甜品、火锅、料理等

    @Schema(description = "所在区域", example = "白云区",
            allowableValues = {"白云区", "天河区", "海珠区", "越秀区", "黄埔区", "番禺区"})
    private String district;    // 地区：白云区、海珠区等

    @Schema(description = "商铺描述", example = "提供正宗粤菜，环境优雅，服务周到")
    private String description; // 商铺描述

    @Schema(description = "创建时间", example = "2025-03-28T14:00:00",
            accessMode = Schema.AccessMode.READ_ONLY) // 只读字段，客户端不能修改
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2025-03-28T14:30:00",
            accessMode = Schema.AccessMode.READ_ONLY) // 只读字段，客户端不能修改
    private LocalDateTime updateTime;

    @Schema(description = "商铺状态：0-关闭，1-营业中", example = "1",
            allowableValues = {"0", "1"})
    private Integer status;     // 商铺状态：0-关闭，1-营业中
}