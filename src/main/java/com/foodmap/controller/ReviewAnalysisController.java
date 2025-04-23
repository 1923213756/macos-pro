package com.foodmap.controller;


import com.foodmap.entity.dto.ApiResponse;
import com.foodmap.service.ai.ReviewAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/api/reviews/analysis")
@RequiredArgsConstructor
public class ReviewAnalysisController {

    private final ReviewAnalysisService reviewAnalysisService;

    /**
     * 获取餐厅评论摘要
     */
    @GetMapping("/summary/{restaurantId}")
    public ResponseEntity<ApiResponse> getReviewSummary(@PathVariable Long restaurantId) {
        try {
            String summary = reviewAnalysisService.getSummary(restaurantId);
            return ResponseEntity.ok(new ApiResponse(true, "获取评论摘要成功", summary));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "获取评论摘要失败: " + e.getMessage()));
        }
    }

    /**
     * 异步获取餐厅评论摘要
     */
    @GetMapping("/summary/{restaurantId}/async")
    public DeferredResult<ResponseEntity<ApiResponse>> getReviewSummaryAsync(@PathVariable Long restaurantId) {
        DeferredResult<ResponseEntity<ApiResponse>> deferredResult = new DeferredResult<>(60000L);

        reviewAnalysisService.generateSummaryAsync(restaurantId)
                .thenAccept(summary -> {
                    deferredResult.setResult(
                            ResponseEntity.ok(new ApiResponse(true, "获取评论摘要成功", summary))
                    );
                })
                .exceptionally(ex -> {
                    deferredResult.setResult(
                            ResponseEntity.status(500)
                                    .body(new ApiResponse(false, "获取评论摘要失败: " + ex.getMessage()))
                    );
                    return null;
                });

        return deferredResult;
    }

    /**
     * 强制刷新餐厅评论摘要
     */
    @PostMapping("/summary/{restaurantId}/refresh")
    public ResponseEntity<ApiResponse> refreshReviewSummary(@PathVariable Long restaurantId) {
        try {
            String summary = reviewAnalysisService.refreshSummary(restaurantId);
            return ResponseEntity.ok(new ApiResponse(true, "评论摘要已刷新", summary));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new ApiResponse(false, "刷新评论摘要失败: " + e.getMessage()));
        }
    }
}