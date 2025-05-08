package com.foodmap.service.ai;

import com.foodmap.mapper.AspectSummaryMapper;
import com.foodmap.mapper.ReviewMapper;
import com.foodmap.entity.pojo.AspectSummary;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AspectSentimentService {

    @Autowired
    private OllamaAiClient ollamaAiClient;  // 添加Ollama客户端依赖

    @Value("${python.service.url:http://localhost:5001}")
    private String pythonServiceUrl;

    private final WebClient webClient;
    private final ReviewMapper reviewMapper;
    private final AspectSummaryMapper aspectSummaryMapper;

    @Autowired
    public AspectSentimentService(ReviewMapper reviewMapper,
                                  AspectSummaryMapper aspectSummaryMapper) {
        // 修改WebClient创建方式，确保URL正确包含协议和端口
        this.pythonServiceUrl = "http://localhost:5001"; // 先硬编码确保格式正确
        this.webClient = WebClient.builder()
                .baseUrl(this.pythonServiceUrl)
                .build();
        this.reviewMapper = reviewMapper;
        this.aspectSummaryMapper = aspectSummaryMapper;

        System.out.println("Python服务URL设置为: " + this.pythonServiceUrl);
    }

    // 在构造函数后添加初始化方法
    @PostConstruct
    public void init() {
        try {
            Map<String, Object> healthStatus = checkPythonServiceHealth();
            System.out.println("Python服务健康检查结果: " + healthStatus);

            if ("success".equals(healthStatus.get("status"))) {
                System.out.println("Python情感分析服务已成功连接");
            } else {
                System.err.println("警告: Python情感分析服务可能不可用，请检查配置与服务状态");
            }
        } catch (Exception e) {
            System.err.println("Python服务健康检查异常: " + e.getMessage());
        }
    }


    /**
     * 定时任务：分析餐厅评论
     */

    //测试阶段删去
    // @Scheduled(cron = "0 0 */3 * * *") // 每3小时执行一次
    public void scheduledAnalysis() {
        // 获取未分析的评论，限制每次处理100条
        List<Map<String, Object>> reviews = reviewMapper.findUnanalyzedReviews(100);
        processReviews(reviews);
    }

    /**
     * 手动触发特定餐厅的评论分析
     */
    public Map<String, Object> analyzeRestaurantReviews(Long restaurantId) {
        // 获取该餐厅最新的100条评论进行分析（不论是否已分析）
        //测试阶段改为1条
        System.out.println("成功发送请求，ID" + restaurantId);
        List<Map<String, Object>> reviews = reviewMapper.getRecentReviewsForAnalysis(restaurantId, 5);

        System.out.println("获取到的评论数量: " + reviews.size());

        if (reviews.isEmpty()) {
            return Map.of("status", "success", "message", "没有可分析的评论");
        }

        // 重置该餐厅评论的分析状态
        reviewMapper.resetAnalysisStatusByRestaurant(restaurantId);

        System.out.println("进入处理评论阶段，revews如下" + reviews);

        // 处理评论
        Map<String, Object> result = processReviews(reviews);
        System.out.println("评论处理完成，结果: " + result);
        result.put("restaurantId", restaurantId);

        return result;
    }

    /**
     * 处理评论列表
     */
    private Map<String, Object> processReviews(List<Map<String, Object>> reviewMaps) {
        if (reviewMaps.isEmpty()) {
            System.out.println("没有需要分析的评论，跳过处理");
            return Map.of("status", "success", "message", "没有需要分析的评论");
        }

        System.out.println("开始处理评论批次，共" + reviewMaps.size() + "条评论");

        try {
            // 提取评论内容和ID
            List<String> reviewContents = new ArrayList<>();
            List<Long> reviewIds = new ArrayList<>();
            Map<String, Long> restaurantIdMap = new HashMap<>();

            for (Map<String, Object> review : reviewMaps) {
                Long id = Long.valueOf(review.get("id").toString());
                String content = (String) review.get("content");
                Long restaurantId = Long.valueOf(review.get("restaurantId").toString());

                reviewIds.add(id);
                reviewContents.add(content);
                restaurantIdMap.put(id.toString(), restaurantId);
            }

            System.out.println("已提取评论信息，准备发送到Python服务进行分析");

            // 准备请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reviews", reviewContents);

            // 调用Python服务分析评论，使用WebClient替代RestTemplate
            System.out.println("===== 发送评论分析请求详情 =====");
            System.out.println("请求URL: " + pythonServiceUrl + "/analyze_batch");
            System.out.println("评论数量: " + reviewContents.size());
            System.out.println("请求内容: " + reviewContents); // 打印所有评论内容
            System.out.println("===== 请求详情结束 =====");

            System.out.println("向" + pythonServiceUrl + "/analyze_batch发送评论分析请求...");
            long startTime = System.currentTimeMillis();

            Map<String, Object> response = webClient.post()
                    .uri("/analyze_batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(java.time.Duration.ofSeconds(500));

            long endTime = System.currentTimeMillis();
            System.out.println("Python服务响应耗时: " + (endTime - startTime) + "ms");

            // 检查响应内容
            if (response == null) {
                System.err.println("Python服务返回空响应");
                return Map.of("status", "error", "message", "Python服务返回空响应");
            }

            System.out.println("Python服务响应包含的键: " + response.keySet());

            // 获取餐厅ID (假设所有评论都来自同一餐厅)
            Long restaurantId = null;
            if (!reviewMaps.isEmpty()) {
                restaurantId = Long.valueOf(reviewMaps.get(0).get("restaurantId").toString());
            }

            // 验证响应结构 - 使用新的结构字段
            if (!response.containsKey("phrase_stats")) {
                System.err.println("Python服务返回的数据结构不完整，缺少phrase_stats字段");
                return Map.of("status", "error", "message", "Python服务返回数据结构不符合预期");
            }

            // 处理分析结果
            System.out.println("开始处理Python返回的分析结果...");
            
            // 获取摘要
            String summary = (String) response.getOrDefault("summary", "无法生成摘要");
            System.out.println("获取到摘要: " + (summary.length() > 50 ? summary.substring(0, 50) + "..." : summary));
            
            // 获取短语统计
            List<Map<String, Object>> phraseStats = (List<Map<String, Object>>) response.get("phrase_stats");
            System.out.println("获取到短语统计: " + phraseStats.size() + " 项");
            
            // 获取总评论数
            int totalReviews = ((Number) response.getOrDefault("total_reviews", reviewMaps.size())).intValue();
            
            if (restaurantId != null) {
                // 将短语统计转换为方面情感统计并保存到数据库
                List<Map<String, Object>> aspectStats = convertPhraseStatsToAspectStats(phraseStats);
                System.out.println("准备更新 " + aspectStats.size() + " 条方面统计数据");
                
                // 更新数据库
                int updatedRows = aspectSummaryMapper.batchUpsertAspectStatistics(restaurantId, aspectStats);
                System.out.println("成功更新 " + updatedRows + " 行方面统计数据");
                
                // 可选：保存摘要到review_summaries表（如果有对应表和mapper）
                // saveReviewSummary(restaurantId, summary, totalReviews);
                
                // 标记评论为已分析

                int updatedCount = reviewMapper.markReviewsAsAnalyzed(reviewIds);
                System.out.println("已将" + updatedCount + "条评论标记为已分析");
                
                return Map.of(
                        "status", "success",
                        "message", "成功分析" + reviewIds.size() + "条评论，更新了餐厅的方面统计",
                        "summary", summary,
                        "aspectStats", aspectStats,
                        "totalReviews", totalReviews
                );
            } else {
                return Map.of(
                        "status", "error",
                        "message", "无法确定评论所属餐厅"
                );
            }

        } catch (Exception e) {
            System.err.println("评论分析过程中发生异常: " + e.getMessage());
            e.printStackTrace();
            return Map.of(
                    "status", "error",
                    "message", "评论分析失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取餐厅的方面情感统计摘要
     */
    public Map<String, Object> getRestaurantSentimentSummary(Long restaurantId) {
        List<AspectSummary> summaries = aspectSummaryMapper.getAspectSummaryByRestaurant(restaurantId);

        if (summaries.isEmpty()) {
            return Map.of("status", "empty", "message", "暂无方面情感分析数据");
        }

        Map<String, Object> summaryText = aspectSummaryMapper.getLatestSummaryText(restaurantId);

        Map<String, Object> result = new HashMap<>();
        result.put("restaurantId", restaurantId);
        result.put("summaryText", summaryText.get("summary"));
        result.put("aspects", summaries);

        return result;
    }

    /**
     * 检查Python服务健康状态
     *
     * @return 包含健康状态与消息的Map
     */
    public Map<String, Object> checkPythonServiceHealth() {
        try {
            System.out.println("正在检查Python服务健康状态: " + pythonServiceUrl + "/health");

            Map<String, Object> response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block(java.time.Duration.ofSeconds(5)); // 添加5秒超时

            if (response != null && "healthy".equals(response.get("status"))) {
                return Map.of(
                        "status", "success",
                        "message", "Python服务运行正常",
                        "details", response
                );
            } else {
                return Map.of(
                        "status", "warning",
                        "message", "Python服务响应异常",
                        "details", response != null ? response : "无响应数据"
                );
            }
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细堆栈便于调试
            return Map.of(
                    "status", "error",
                    "message", "Python服务连接失败: " + e.getMessage(),
                    "exception", e.getClass().getName()
            );
        }
    }

    /**
     * 测试Python连接并进行简单分析
     */
    public Map<String, Object> testPythonConnection() {
        try {
            // 准备简单的测试评论
            String testReview = "测试评论，环境很好，服务态度一般";
            Map<String, Object> requestBody = Map.of("text", testReview);

            System.out.println("发送测试请求到Python服务: " + pythonServiceUrl + "/analyze");

            Map<String, Object> response = webClient.post()
                    .uri("/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block(java.time.Duration.ofSeconds(10)); // 10秒超时

            // 提取分析结果中的关键信息
            if (response != null && response.containsKey("unique_aspect_results")) {
                List<Map<String, Object>> aspectResults =
                        (List<Map<String, Object>>) response.get("unique_aspect_results");

                return Map.of(
                        "status", "success",
                        "message", "Python服务分析功能正常",
                        "testReview", testReview,
                        "aspectResults", aspectResults,
                        "summary", response.get("summary")
                );
            } else {
                return Map.of(
                        "status", "warning",
                        "message", "Python服务返回异常结果",
                        "response", response != null ? response : "无响应数据"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "status", "error",
                    "message", "测试连接失败: " + e.getMessage(),
                    "exception", e.getClass().getName()
            );
        }
    }
    /**
     * 将短语统计转换为方面情感统计
     */
    private List<Map<String, Object>> convertPhraseStatsToAspectStats(List<Map<String, Object>> phraseStats) {
        List<Map<String, Object>> aspectStats = new ArrayList<>();

        for (Map<String, Object> phraseStat : phraseStats) {
            String phrase = (String) phraseStat.get("phrase");
            int count = ((Number) phraseStat.get("count")).intValue();
            String sentiment = (String) phraseStat.get("sentiment");
            double confidence = ((Number) phraseStat.get("confidence")).doubleValue();

            // 创建方面统计对象
            Map<String, Object> aspectStat = new HashMap<>();
            aspectStat.put("aspect", phrase);  // 使用短语作为方面名称

            int positiveCount = 0;
            int negativeCount = 0;

            // 根据情感确定正面/负面计数
            if ("好".equals(sentiment)) {
                positiveCount = count;
            } else if ("差".equals(sentiment)) {
                negativeCount = count;
            } else {
                // 中性评价，可以选择如何处理
                // 这里我们将中性计为一半正面一半负面
                positiveCount = count / 2;
                negativeCount = count / 2;
            }

            aspectStat.put("positive", positiveCount);
            aspectStat.put("negative", negativeCount);
            aspectStat.put("total", count);

            // 计算百分比 (正面评价占总数的百分比)
            double percentage = (count > 0) ? ((double) positiveCount / count) * 100.0 : 0.0;
            aspectStat.put("positivePercentage", percentage);

            aspectStats.add(aspectStat);
        }

        return aspectStats;
    }
}