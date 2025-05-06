package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("aspect_summary")
public class AspectSummary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long restaurantId;

    private String aspect;

    private Integer positiveCount;

    private Integer negativeCount;

    private Integer totalCount;

    private BigDecimal positivePercentage;

    private LocalDateTime lastUpdated;
}