package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "商铺实体")
public class Shop {
    @Schema(description = "商铺ID", example = "1001")
    private Long shopId;

    @NotBlank(message = "商铺名称不能为空")
    @Size(min = 2, max = 50, message = "商铺名称长度必须在2-50个字符之间")
    @Schema(description = "商铺名称", example = "美味小厨")
    private String shopName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20个字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$",
            message = "密码至少包含一个大写字母、一个小写字母和一个数字")
    @Schema(description = "商铺密码", example = "Password123", hidden = true)
    private String password;

    @NotBlank(message = "商铺地址不能为空")
    @Size(max = 200, message = "地址长度不能超过200个字符")
    @Schema(description = "商铺地址", example = "广州市天河区天河路385号")
    private String address;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^\\d{3,4}-\\d{7,8}$|^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Schema(description = "联系电话", example = "020-12345678")
    private String contactTel;

    @Size(max = 100, message = "营业时间长度不能超过100个字符")
    @Schema(description = "营业时间", example = "周一至周日 09:00-22:00")
    private String businessHours;

    @NotBlank(message = "商铺分类不能为空")
    @Schema(description = "商铺分类", example = "中餐",
            allowableValues = {"中餐", "西餐", "火锅", "烧烤", "甜品", "快餐"})
    private String category;

    @NotBlank(message = "所在区域不能为空")
    @Schema(description = "所在区域", example = "白云区",
            allowableValues = {"白云区", "天河区", "海珠区", "越秀区", "黄埔区", "番禺区"})
    private String district;

    @Size(max = 500, message = "商铺描述不能超过500个字符")
    @Schema(description = "商铺描述", example = "提供正宗粤菜，环境优雅，服务周到")
    private String description;

    @Schema(description = "创建时间", example = "2025-03-28T14:00:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2025-03-28T14:30:00",
            accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateTime;

    @NotNull(message = "商铺状态不能为空")
    @Min(value = 0, message = "商铺状态值必须为0或1")
    @Max(value = 1, message = "商铺状态值必须为0或1")
    @Schema(description = "商铺状态：0-关闭，1-营业中", example = "1",
            allowableValues = {"0", "1"})
    private Integer status;

    @DecimalMin(value = "0.0", message = "综合评分不能小于0")
    @DecimalMax(value = "5.0", message = "综合评分不能大于5")
    @Digits(integer = 1, fraction = 1, message = "评分格式为一位整数，一位小数")
    @Schema(description = "综合评分", example = "4.8")
    private Float compositeScore;

    @DecimalMin(value = "0.0", message = "环境评分不能小于0")
    @DecimalMax(value = "5.0", message = "环境评分不能大于5")
    @Digits(integer = 1, fraction = 1, message = "评分格式为一位整数，一位小数")
    @Schema(description = "环境评分", example = "4.8")
    private Float environmentScore;

    @DecimalMin(value = "0.0", message = "服务评分不能小于0")
    @DecimalMax(value = "5.0", message = "服务评分不能大于5")
    @Digits(integer = 1, fraction = 1, message = "评分格式为一位整数，一位小数")
    @Schema(description = "服务评分", example = "4.8")
    private Float serviceScore;

    @DecimalMin(value = "0.0", message = "味道评分不能小于0")
    @DecimalMax(value = "5.0", message = "味道评分不能大于5")
    @Digits(integer = 1, fraction = 1, message = "评分格式为一位整数，一位小数")
    @Schema(description = "味道评分", example = "4.8")
    private Float tasteScore;

    @Setter
    @TableField("review_count")  // 如果您使用MyBatis-Plus
    private Long reviewCount;

    // Getter和Setter方法
    public Long getReviewCount() {
        return reviewCount == null ? 0L : reviewCount;
    }

}