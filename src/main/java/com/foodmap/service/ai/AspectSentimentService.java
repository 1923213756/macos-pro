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
        List<Map<String, Object>> reviews = reviewMapper.getRecentReviewsForAnalysis(restaurantId, 1);

        if (reviews.isEmpty()) {
            return Map.of("status", "success", "message", "没有可分析的评论");
        }

        // 重置该餐厅评论的分析状态
        reviewMapper.resetAnalysisStatusByRestaurant(restaurantId);

        // 处理评论
        Map<String, Object> result = processReviews(reviews);
        result.put("restaurantId", restaurantId);

        return result;
    }

    /**
     * 处理评论列表
     */
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
                Long restaurantId = Long.valueOf(review.get("restaurant_id").toString());

                reviewIds.add(id);
                reviewContents.add(content);
                restaurantIdMap.put(id.toString(), restaurantId);
            }

            System.out.println("已提取评论信息，准备发送到Python服务进行分析");

            // 准备请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reviews", reviewContents);

            // 调用Python服务分析评论，使用WebClient替代RestTemplate
            System.out.println("向" + pythonServiceUrl + "/analyze_batch发送评论分析请求...");
            long startTime = System.currentTimeMillis();

            Map<String, Object> response = webClient.post()
                    .uri("/analyze_batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block(java.time.Duration.ofSeconds(60)); // 增加超时时间到60秒，大批量评论需要更长时间

            long endTime = System.currentTimeMillis();
            System.out.println("Python服务响应耗时: " + (endTime - startTime) + "ms");

            // 检查响应内容
            if (response == null) {
                System.err.println("Python服务返回空响应");
                return Map.of("status", "error", "message", "Python服务返回空响应");
            }

            System.out.println("Python服务响应包含的键: " + response.keySet());

            // 验证响应结构
            if (!response.containsKey("aspect_stats") || !response.containsKey("individual_results")) {
                System.err.println("Python服务返回的数据结构不完整，缺少必要字段");
                return Map.of("status", "error", "message", "Python服务返回数据结构不符合预期");
            }

            // 处理分析结果
            System.out.println("开始处理Python返回的分析结果...");
            if (response.containsKey("aspect_stats")) {
                // 按餐厅分组更新方面统计
                Map<Long, Map<String, Map<String, Integer>>> restaurantAspectStats = new HashMap<>();

                // 从个别结果中获取方面情感数据
                List<Map<String, Object>> individualResults =
                        (List<Map<String, Object>>) response.get("individual_results");

                System.out.println("收到" + individualResults.size() + "条评论的个别分析结果");

                // 用于存储每个餐厅的摘要文本
                Map<Long, String> restaurantSummaries = new HashMap<>();

                // 处理每条评论的分析结果
                for (int i = 0; i < individualResults.size(); i++) {
                    Map<String, Object> result = individualResults.get(i);
                    Long reviewId = reviewIds.get(i);
                    Long restaurantId = restaurantIdMap.get(reviewId.toString());

                    // 获取分析结果
                    Map<String, Object> analysis = (Map<String, Object>) result.get("analysis");

                    // 提取并保存摘要文本
                    if (analysis.containsKey("summary") && analysis.get("summary") != null) {
                        String summary = (String) analysis.get("summary");
                        // 更新餐厅的摘要文本（可能多个评论，取最后一个或合并）
                        restaurantSummaries.put(restaurantId, summary);
                    }

                    List<Map<String, Object>> aspectResults =
                            (List<Map<String, Object>>) analysis.get("unique_aspect_results");

                    if (aspectResults == null || aspectResults.isEmpty()) {
                        System.out.println("警告：评论ID " + reviewId + " 没有提取到方面情感结果");
                        continue;
                    }

                    // 确保餐厅的统计数据初始化
                    if (!restaurantAspectStats.containsKey(restaurantId)) {
                        restaurantAspectStats.put(restaurantId, new HashMap<>());
                    }

                    // 更新该餐厅的方面统计
                    for (Map<String, Object> aspectResult : aspectResults) {
                        String aspect = (String) aspectResult.get("aspect");
                        String sentiment = (String) aspectResult.get("sentiment");
                        String text = (String) aspectResult.get("text");
                        Double confidence = Double.valueOf(aspectResult.get("confidence").toString());

                        // 更新统计计数
                        Map<String, Map<String, Integer>> aspectStats = restaurantAspectStats.get(restaurantId);
                        if (!aspectStats.containsKey(aspect)) {
                            aspectStats.put(aspect, new HashMap<>());
                            aspectStats.get(aspect).put("好", 0);
                            aspectStats.get(aspect).put("差", 0);
                        }

                        // 增加对应情感计数
                        aspectStats.get(aspect).put(sentiment,
                                aspectStats.get(aspect).get(sentiment) + 1);

                        // 保存证据文本到数据库（这部分代码可以实现）
                        // TODO: 实现证据文本的存储
                        System.out.println("餐厅ID " + restaurantId + " 方面[" + aspect + "] 情感[" + sentiment +
                                "] 证据文本: " + text + " 置信度: " + confidence);
                    }
                }

                // 更新数据库中的方面统计数据
                System.out.println("开始更新数据库中的情感分析结果...");
                int restaurantCount = 0;
                int aspectCount = 0;

                for (Map.Entry<Long, Map<String, Map<String, Integer>>> entry : restaurantAspectStats.entrySet()) {
                    Long restaurantId = entry.getKey();
                    Map<String, Map<String, Integer>> aspectStats = entry.getValue();

                    // 转换为数据库更新所需格式
                    List<Map<String, Object>> dbAspectStats = new ArrayList<>();

                    for (Map.Entry<String, Map<String, Integer>> aspectEntry : aspectStats.entrySet()) {
                        String aspect = aspectEntry.getKey();
                        Map<String, Integer> counts = aspectEntry.getValue();

                        Map<String, Object> stat = new HashMap<>();
                        stat.put("aspect", aspect);
                        stat.put("positive", counts.get("好"));
                        stat.put("negative", counts.get("差"));
                        stat.put("total", counts.get("好") + counts.get("差"));

                        dbAspectStats.add(stat);
                        aspectCount++;
                    }

                    // 更新数据库
                    aspectSummaryMapper.batchUpsertAspectStatistics(restaurantId, dbAspectStats);


                    restaurantCount++;
                }

                System.out.println("已更新" + restaurantCount + "家餐厅的" + aspectCount + "项方面情感统计");

                // 标记评论为已分析
                String reviewIdList = reviewIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                int updatedCount = reviewMapper.markReviewsAsAnalyzed(reviewIdList);
                System.out.println("已将" + updatedCount + "条评论标记为已分析");

                return Map.of(
                        "status", "success",
                        "message", "成功分析" + reviewIds.size() + "条评论，更新了" + restaurantCount + "家餐厅的情感统计"
                );
            }

            System.err.println("Python服务返回无效结果，缺少aspect_stats字段");
            return Map.of("status", "error", "message", "Python服务返回无效结果");

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
     * @return 包含健康状态与消息的Map
     */
    public Map<String, Object> checkPythonServiceHealth() {
        try {
            System.out.println("正在检查Python服务健康状态: " + pythonServiceUrl + "/health");

            Map<String, Object> response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
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
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
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
}