package com.foodmap.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentAnalysisService {

    private final WebClient webClient;

    /**
     * 调用外部服务进行评论摘要
     * @param comments 用户评论列表
     * @return 关键词与统计结果
     */
    public Mono<Map<String, Integer>> getSummary(List<String> comments) {
        return webClient.post()
                .uri("/summarize") // 调用外部 /summarize 接口
                .bodyValue(comments)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Integer>>() {}); // 返回值为 Map<String, Integer>
    }

    /**
     * 调用外部服务进行情感分析
     * @param comment 单条用户评论
     * @return 情感分析结果
     */
    public Mono<String> analyzeSentiment(String comment) {
        return webClient.post()
                .uri("/sentiment") // 调用外部 /sentiment 接口
                .bodyValue(comment)
                .retrieve()
                .bodyToMono(String.class); // 返回值为情感分类结果
    }
}