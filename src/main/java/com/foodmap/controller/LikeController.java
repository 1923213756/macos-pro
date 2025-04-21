package com.foodmap.controller;


import com.foodmap.entity.dto.ApiResponse;
import com.foodmap.entity.dto.LikeCreateDTO;
import com.foodmap.service.LikeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping("/toggle")
    public ResponseEntity<ApiResponse> toggleLike(@Valid @RequestBody LikeCreateDTO dto) {
        boolean isLiked = likeService.toggleLike(dto);

        String message = isLiked ? "点赞成功" : "取消点赞成功";
        return ResponseEntity.ok(new ApiResponse(true, message));
    }

    @GetMapping("/check/{reviewId}")
    public ResponseEntity<ApiResponse> checkLikeStatus(@PathVariable Long reviewId) {
        boolean hasLiked = likeService.hasUserLiked(reviewId);
        return ResponseEntity.ok(new ApiResponse(true, hasLiked ? "已点赞" : "未点赞", hasLiked));
    }
}