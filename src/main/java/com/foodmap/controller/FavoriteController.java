package com.foodmap.controller;

import com.foodmap.common.response.ResponseResult;
import com.foodmap.entity.dto.FavoriteCreateDTO;
import com.foodmap.service.FavoriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/toggle")
    public ResponseResult<Boolean> toggleFavorite(@Valid @RequestBody FavoriteCreateDTO dto) {
        boolean isFavorited = favoriteService.toggleFavorite(dto);
        String message = isFavorited ? "收藏成功" : "取消收藏成功";
        return ResponseResult.success(message, isFavorited);
    }

    @GetMapping("/check/{shopId}")
    public ResponseResult<Boolean> checkFavoriteStatus(@PathVariable Long shopId) {
        boolean hasFavorited = favoriteService.hasUserFavorited(shopId);
        return ResponseResult.success(hasFavorited);
    }
}