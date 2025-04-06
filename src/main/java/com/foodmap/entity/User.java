package com.foodmap.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户实体")
public class User {
    @Schema(description = "用户ID", example = "1001")
    private Long id;

    @Schema(description = "用户名", example = "foodlover")
    private String userName;

    @Schema(description = "密码(加密后)", example = "********", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Schema(description = "手机号码", example = "13812345678")
    private String phone; // 改为String类型，因为手机号不用于计算且可能有前导零

    @Schema(description = "注册时间", example = "2025-03-31T12:40:55")
    private LocalDateTime registerTime;

    @Schema(description = "最后登录时间", example = "2025-03-31T12:40:55")
    private LocalDateTime lastLoginTime;

    @Schema(description = "账号状态: 0-禁用, 1-正常", example = "1", allowableValues = {"0", "1"})
    private Integer status = 1; // 默认为正常状态
}