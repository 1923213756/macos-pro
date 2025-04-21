package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.*;
import com.foodmap.entity.pojo.Shop;
import com.foodmap.entity.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reviews")
public class Review {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String content;

    private Integer rating;//综合评分

    @TableField("environment_rating")
    private Integer environmentRating; // 环境评分

    @TableField("service_rating")
    private Integer serviceRating; // 服务评分

    @TableField("taste_rating")
    private Integer tasteRating; // 口味评分

    @TableField("user_id")
    private Long userId;

    @TableField("restaurant_id")
    private Long restaurantId;

    @TableField("like_count")
    @Builder.Default
    private Integer likeCount = 0;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String status;

    @TableField(exist = false)
    private User user;

    @TableField(exist = false)
    private Shop shop;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_HIDDEN = "HIDDEN";
    public static final String STATUS_DELETED = "DELETED";
}