package com.foodmap.controller;

import com.foodmap.service.ai.AspectSentimentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
public class AspectSentimentController {

    private final AspectSentimentService aspectSentimentService;

    @Autowired
    public AspectSentimentController(AspectSentimentService aspectSentimentService) {
        this.aspectSentimentService = aspectSentimentService;
    }


    /**
     * 获取餐厅的方面情感统计结果
     * @param restaurantId 餐厅ID
     */
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<Map<String, Object>> getRestaurantSentiment(@PathVariable Long restaurantId) {
        Map<String, Object> summary = aspectSentimentService.getRestaurantSentimentSummary(restaurantId);
        return ResponseEntity.ok(summary);
    }

    /**
     * 手动触发特定餐厅的评论分析
     * @param restaurantId 餐厅ID
     */
    @PostMapping("/analyze/restaurant/{restaurantId}")
    public ResponseEntity<Map<String, Object>> analyzeRestaurant(@PathVariable Long restaurantId) {
        Map<String, Object> result = aspectSentimentService.analyzeRestaurantReviews(restaurantId);
        return ResponseEntity.ok(result);
    }

    /**
     * 检查Python服务健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> healthStatus = aspectSentimentService.checkPythonServiceHealth();
        return ResponseEntity.ok(healthStatus);
    }

    /**
     * 测试与Python服务的连接并进行简单分析
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testPythonService() {
        Map<String, Object> result = aspectSentimentService.testPythonConnection();
        return ResponseEntity.ok(result);
    }
}