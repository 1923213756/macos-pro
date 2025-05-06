package com.foodmap.controller;

import com.foodmap.service.ai.CommentAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentAnalysisController {

    private final CommentAnalysisService commentAnalysisService;

    /**
     * 评论数据摘要
     * @param comments 用户评论列表
     * @return 关键词统计结果
     */
    @PostMapping("/summarize")
    public Mono<Map<String, Integer>> summarizeComments(@RequestBody List<String> comments) {
        return commentAnalysisService.getSummary(comments);
    }

    /**
     * 单条评论情感分析
     * @param comment 用户评论
     * @return 情感分类结果
     */
    @PostMapping("/sentiment")
    public Mono<String> analyzeCommentSentiment(@RequestBody String comment) {
        return commentAnalysisService.analyzeSentiment(comment);
    }
}