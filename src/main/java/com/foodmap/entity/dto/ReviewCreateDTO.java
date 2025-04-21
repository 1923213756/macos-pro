package com.foodmap.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateDTO {

    @NotBlank(message = "评价内容不能为空")
    private String content;

    // 可选综合评分字段，可由系统根据三个维度评分计算得出
    private Integer rating;

    @NotNull(message = "环境评分不能为空")
    @Min(value = 1, message = "环境评分最小为1")
    @Max(value = 5, message = "环境评分最大为5")
    private Integer environmentRating;

    @NotNull(message = "服务评分不能为空")
    @Min(value = 1, message = "服务评分最小为1")
    @Max(value = 5, message = "服务评分最大为5")
    private Integer serviceRating;

    @NotNull(message = "口味评分不能为空")
    @Min(value = 1, message = "口味评分最小为1")
    @Max(value = 5, message = "口味评分最大为5")
    private Integer tasteRating;

    @NotNull(message = "餐厅ID不能为空")
    private Long restaurantId;

    // 用户ID通常由后端从当前登录用户中获取，而不是由前端传入
    // 但如果您的系统设计需要前端传入，可以取消下面的注释
    // private Long userId;
}