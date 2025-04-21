package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
@Schema(description = "用户实体")
public class User {
    @TableId(value = "userId", type = IdType.AUTO)
    @Schema(description = "用户ID", example = "1001")
    private Long userId;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 30, message = "用户名长度必须在3-30个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    @TableField("userName")
    @Schema(description = "用户名", example = "foodlover")
    private String userName;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20个字符之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$",
            message = "密码至少包含一个大写字母、一个小写字母和一个数字")
    @TableField("password")
    @Schema(description = "密码(加密后)", example = "********", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @TableField("phone")
    @Schema(description = "手机号码", example = "13812345678")
    private String phone;

    @TableField("createTime")
    @Schema(description = "注册时间", example = "2025-03-31T12:40:55", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;

    @TableField("updateTime")
    @Schema(description = "更新时间", example = "2025-03-31T12:40:55", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updateTime;

    @Min(value = 0, message = "状态值必须为0或1")
    @Max(value = 1, message = "状态值必须为0或1")
    @TableField("status")
    @Schema(description = "账号状态: 0-禁用, 1-正常", example = "1", allowableValues = {"0", "1"})
    private Integer status = 1; // 默认为正常状态
}