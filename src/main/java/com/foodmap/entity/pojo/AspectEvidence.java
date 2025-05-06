package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("aspect_summary_evidence")
public class AspectEvidence {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long summaryId;

    private Long reviewId;

    private String aspect;

    private String sentiment;

    private String evidenceText;

    private BigDecimal confidence;

    private LocalDateTime createdAt;
}