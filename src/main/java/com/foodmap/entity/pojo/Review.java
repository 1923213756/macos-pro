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

    private Integer compositeScore;//综合评分

    @TableField("environmentScore")
    private Integer environmentScore; // 环境评分

    @TableField("serviceScore")
    private Integer serviceScore; // 服务评分

    @TableField("tasteScore")
    private Integer tasteScore; // 口味评分

    @TableField("userId")
    private Long userId;

    @TableField("restaurantId")
    private Long restaurantId;

    @TableField("likeCount")
    @Builder.Default
    private Integer likeCount = 0;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updatedAt", fill = FieldFill.INSERT_UPDATE)
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