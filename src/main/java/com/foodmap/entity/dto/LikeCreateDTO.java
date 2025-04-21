package com.foodmap.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikeCreateDTO {

    @NotNull(message = "评论ID不能为空")
    private Long reviewId;

    private String type = "LIKE";
}