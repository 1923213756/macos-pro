package com.foodmap.controller;

import com.foodmap.entity.dto.ApiResponse;
import com.foodmap.service.ai.ReviewGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/guide")
@RequiredArgsConstructor
public class ReviewGuideController {

    private final ReviewGuideService reviewGuideService;

    /**
     * 根据菜品名称获取评论引导模板
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getReviewGuide(@RequestParam String dish) {
        try {
            // 检查菜品名是否为空
            if (dish == null || dish.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "菜品名称不能为空"));
            }

            // 调用服务层生成引导
            String guide = reviewGuideService.generateGuide(dish.trim());
            return ResponseEntity.ok(new ApiResponse(true, "获取评论引导成功", guide));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "获取评论引导失败: " + e.getMessage()));
        }
    }
}