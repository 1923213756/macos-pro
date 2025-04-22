package com.foodmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodmap.entity.dto.FavoriteCreateDTO;
import com.foodmap.entity.pojo.Favorite;
import com.foodmap.entity.pojo.User;
import com.foodmap.mapper.FavoriteMapper;
import com.foodmap.service.FavoriteService;
import com.foodmap.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final UserService userService;

    @Override
    @Transactional
    public boolean toggleFavorite(FavoriteCreateDTO dto) {
        User currentUser = userService.getCurrentUser();

        LambdaQueryWrapper<Favorite> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Favorite::getUserId, currentUser.getUserId())
                .eq(Favorite::getShopId, dto.getShopId());

        Favorite existingFavorite = favoriteMapper.selectOne(queryWrapper);

        if (existingFavorite != null) {
            // 已收藏，则取消收藏
            favoriteMapper.deleteById(existingFavorite.getId());
            return false;
        } else {
            // 未收藏，则添加收藏
            Favorite favorite = Favorite.builder()
                    .userId(currentUser.getUserId())
                    .shopId(dto.getShopId())
                    .build();
            favoriteMapper.insert(favorite);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserFavorited(Long shopId) {
        User currentUser = userService.getCurrentUser();
        Integer count = favoriteMapper.checkUserFavorited(currentUser.getUserId(), shopId);
        return count != null && count > 0;
    }
}