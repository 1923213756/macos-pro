package com.foodmap.controller;

import com.foodmap.entity.dto.ApiResponse;
import com.foodmap.service.ai.ReviewGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/guide")
@RequiredArgsConstructor
public class ReviewGuideController {

    private final ReviewGuideService reviewGuideService;


    /**
     * 获取评论引导模板
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getReviewGuide(
            @RequestParam(required = false) String dish,
            @RequestParam(required = false) String type) {

        try {
            String guide = reviewGuideService.generateGuide(dish, type);
            return ResponseEntity.ok(new ApiResponse(true, "获取评论引导成功", guide));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "获取评论引导失败: " + e.getMessage()));
        }
    }
}