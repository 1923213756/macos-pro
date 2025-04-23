package com.foodmap.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_histories")
public class ChatHistory {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("userId")
    private Long userId;

    @TableField("restaurantId")
    private Long restaurantId;

    @TableField("sessionId")
    private String sessionId;

    @TableField("isUserMessage")
    private Boolean isUserMessage;

    private String content;

    @TableField("createdAt")
    private LocalDateTime createdAt;
}