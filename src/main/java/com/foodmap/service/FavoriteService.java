package com.foodmap.service;

import com.foodmap.entity.dto.FavoriteCreateDTO;

public interface FavoriteService {

    /**
     * 收藏或取消收藏商铺
     */
    boolean toggleFavorite(FavoriteCreateDTO dto);

    /**
     * 检查用户是否已收藏商铺
     */
    boolean hasUserFavorited(Long shopId);
}