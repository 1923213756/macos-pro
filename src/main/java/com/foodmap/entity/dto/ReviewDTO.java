package com.foodmap.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Long id;
    private String content;

    // 综合评分
    private Integer compositeScore;

    // 新增三个维度评分
    private Integer environmentScore;  // 环境评分
    private Integer serviceScore;      // 服务评分
    private Integer tasteScore;        // 口味评分

    private Long userId;
    private String username;
    private String userAvatar;
    private Long restaurantId;
    private String restaurantName;
    private Integer likeCount;
    private boolean userLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
}