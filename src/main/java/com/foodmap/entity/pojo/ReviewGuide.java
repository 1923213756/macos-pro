package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_guides")
public class ReviewGuide {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("dishName")
    private String dishName;

    @TableField("restaurantType")
    private String restaurantType;

    @TableField("guideContent")
    private String guideContent;

    @TableField("createdAt")
    private LocalDateTime createdAt;

    @TableField("updatedAt")
    private LocalDateTime updatedAt;
}