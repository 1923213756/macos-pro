package com.foodmap.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商铺信息更新DTO")
public class ShopInfoUpdateDTO {
    @NotNull(message = "商铺ID不能为空")
    @Min(value = 1, message = "商铺ID必须大于0")
    @Schema(description = "商铺ID", example = "1001")
    private Long shopId;

    @Size(min = 2, max = 50, message = "商铺名称长度必须在2-50个字符之间")
    @Schema(description = "商铺名称", example = "美味小厨")
    private String shopName;

    @Size(max = 200, message = "地址长度不能超过200个字符")
    @Schema(description = "商铺地址", example = "广州市天河区天河路385号")
    private String address;

//    @Pattern(regexp = "^\\d{3,4}-\\d{7,8}$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话", example = "020-12345678")
    private String contactTel;

    @Size(max = 100, message = "营业时间长度不能超过100个字符")
    @Schema(description = "营业时间", example = "周一至周日 09:00-22:00")
    private String businessHours;

    @Schema(description = "商铺分类", example = "中餐",
            allowableValues = {"中餐", "西餐", "火锅", "烧烤", "甜品", "快餐"})
    private String category;

    @Schema(description = "所在区域", example = "白云区",
            allowableValues = {"白云区", "天河区", "海珠区", "越秀区", "黄埔区", "番禺区"})
    private String district;

    @Size(max = 500, message = "商铺描述不能超过500个字符")
    @Schema(description = "商铺描述", example = "提供正宗粤菜，环境优雅，服务周到")
    private String description;

    @Min(value = 0, message = "商铺状态值必须为0或1")
    @Max(value = 1, message = "商铺状态值必须为0或1")
    @Schema(description = "商铺状态：0-关闭，1-营业中", example = "1",
            allowableValues = {"0", "1"})
    private Integer status;

}