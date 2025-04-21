package com.foodmap.service;

import com.foodmap.entity.dto.LikeCreateDTO;

public interface LikeService {

    /**
     * 点赞或取消点赞
     */
    boolean toggleLike(LikeCreateDTO dto);

    /**
     * 检查用户是否已点赞
     */
    boolean hasUserLiked(Long reviewId);
}