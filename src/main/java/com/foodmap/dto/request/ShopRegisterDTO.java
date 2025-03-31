package com.foodmap.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "商铺注册请求")
public class ShopRegisterDTO {

    @NotBlank(message = "商铺名称不能为空")
    @Size(min = 2, max = 50, message = "商铺名称长度必须在2-50个字符之间")
    @Schema(description = "商铺名称", example = "美味小厨", required = true)
    private String shopName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    @Schema(description = "商铺密码", example = "password123", required = true)
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "password123", required = true)
    private String confirmPassword;

    @NotBlank(message = "商铺地址不能为空")
    @Schema(description = "商铺地址", example = "广州市天河区天河路385号", required = true)
    private String address;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^\\d{3,4}-?\\d{7,8}$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话", example = "020-12345678", required = true)
    private String contactTel;

    @Schema(description = "营业时间", example = "周一至周日 09:00-22:00")
    private String businessHours;

    @NotBlank(message = "商铺分类不能为空")
    @Schema(description = "商铺分类", example = "中餐",
            allowableValues = {"中餐", "西餐", "火锅", "烧烤", "甜品", "快餐"}, required = true)
    private String category;

    @NotBlank(message = "所在区域不能为空")
    @Schema(description = "所在区域", example = "白云区",
            allowableValues = {"白云区", "天河区", "海珠区", "越秀区", "黄埔区", "番禺区"}, required = true)
    private String district;

    @Schema(description = "商铺描述", example = "提供正宗粤菜，环境优雅，服务周到")
    private String description;
}