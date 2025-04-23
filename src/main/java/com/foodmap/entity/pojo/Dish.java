package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("dishes")
public class Dish {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("restaurantId")
    private Long restaurantId;

    private String name;

    private String description;

    // 使用BigDecimal表示货币值，更精确
    private BigDecimal price;

    private String image;

    @TableField("isSpecial")
    private Integer isSpecial;

    @TableField("isAvailable")
    private Integer isAvailable;

    @TableField("createdAt")
    private LocalDateTime createdAt;

    @TableField("updatedAt")
    private LocalDateTime updatedAt;
}