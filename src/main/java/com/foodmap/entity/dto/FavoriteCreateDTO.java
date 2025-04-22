package com.foodmap.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteCreateDTO {

    @NotNull(message = "商铺ID不能为空")
    private Long shopId;
}