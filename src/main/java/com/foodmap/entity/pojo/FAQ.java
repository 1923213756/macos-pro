package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("faqs")
public class FAQ {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long restaurantId;
    private String question;
    private String answer;

    private String keywords;

    @TableField("vector_id")
    private String vectorId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}