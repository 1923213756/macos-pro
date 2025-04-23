package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("likes")
public class Like {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField("reviewId")
    private Long reviewId;

    private String type;

    @TableField(value = "createdAt", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_DISLIKE = "DISLIKE";
}