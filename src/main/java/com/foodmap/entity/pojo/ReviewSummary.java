package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("review_summaries")
public class ReviewSummary {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long restaurantId;

    private String summary;

    @TableField("generatedAt")
    private LocalDateTime generatedAt;

    @TableField("reviewCount")
    private Integer reviewCount;

    private String mentionedDishes;

    @TableField("mainSentiment")
    private String mainSentiment;
}