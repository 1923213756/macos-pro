package com.foodmap.service.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.foodmap.entity.pojo.Dish;
import com.foodmap.entity.pojo.Review;
import com.foodmap.entity.pojo.ReviewSummary;
import com.foodmap.mapper.DishMapper;
import com.foodmap.mapper.ReviewMapper;
import com.foodmap.mapper.ReviewSummaryMapper;
import com.foodmap.service.ai.OllamaAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewAnalysisService {

    private final ReviewMapper reviewMapper;
    private final DishMapper dishMapper;
    private final ReviewSummaryMapper reviewSummaryMapper;
    private final OllamaAiClient ollamaAiClient;

    /**
     * 获取餐厅评论摘要，有缓存
     */
    @Cacheable(value = "reviewSummaries", key = "#restaurantId", unless = "#result == null")
    public String getSummary(Long restaurantId) {
        ReviewSummary existingSummary = reviewSummaryMapper.findByRestaurantId(restaurantId);
        long currentReviewCount = reviewMapper.countByRestaurantId(restaurantId);

        // 如果已有摘要且评论数量未变，则直接返回
        if (existingSummary != null && existingSummary.getReviewCount() == currentReviewCount) {
            return existingSummary.getSummary();
        }

        // 否则重新生成摘要
        return generateSummary(restaurantId);
    }

    /**
     * 强制刷新评论摘要
     */
    @CacheEvict(value = "reviewSummaries", key = "#restaurantId")
    public String refreshSummary(Long restaurantId) {
        return generateSummary(restaurantId);
    }

    /**
     * 异步生成评论摘要
     */
    @Async
    public CompletableFuture<String> generateSummaryAsync(Long restaurantId) {
        return CompletableFuture.supplyAsync(() -> generateSummary(restaurantId));
    }

    /**
     * 生成评论摘要的主要方法
     */
    private String generateSummary(Long restaurantId) {
        // 获取最近的评论(最多100条)
        Page<Review> page = new Page<>(1, 100);
        List<Review> reviews = reviewMapper.findRecentByRestaurantId(restaurantId, page);

        if (reviews.isEmpty()) {
            return "该餐厅暂无评论数据，无法生成摘要。";
        }

        // 获取菜品数据
        List<Dish> dishes = dishMapper.findByRestaurantId(restaurantId);
        List<String> dishNames = dishes.stream()
                .map(Dish::getName)
                .collect(Collectors.toList());

        // 分析评论内容
        Map<String, Object> analysisData = analyzeReviews(reviews, dishNames);

        // 构建提示词
        String prompt = buildSummaryPrompt(analysisData, reviews);

        // 调用AI生成摘要
        String summary = ollamaAiClient.generateText(prompt);

        // 保存或更新摘要
        saveOrUpdateSummary(restaurantId, summary, reviews.size(), analysisData);

        return summary;
    }

    /**
     * 分析评论数据
     */
    private Map<String, Object> analyzeReviews(List<Review> reviews, List<String> knownDishes) {
        Map<String, Object> result = new HashMap<>();

        // 1. 提取评论文本
        List<String> reviewTexts = reviews.stream()
                .map(Review::getContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2. 计算平均评分
        DoubleSummaryStatistics tasteStats = reviews.stream()
                .filter(r -> r.getTasteScore() != null)
                .mapToDouble(Review::getTasteScore)
                .summaryStatistics();

        DoubleSummaryStatistics envStats = reviews.stream()
                .filter(r -> r.getEnvironmentScore() != null)
                .mapToDouble(Review::getEnvironmentScore)
                .summaryStatistics();

        DoubleSummaryStatistics serviceStats = reviews.stream()
                .filter(r -> r.getServiceScore() != null)
                .mapToDouble(Review::getServiceScore)
                .summaryStatistics();

        result.put("avgTasteScore", tasteStats.getAverage());
        result.put("avgEnvironmentScore", envStats.getAverage());
        result.put("avgServiceScore", serviceStats.getAverage());

        // 3. 统计菜品提及次数
        Map<String, Integer> dishMentions = countDishMentions(reviewTexts, knownDishes);
        result.put("dishMentions", dishMentions);

        // 4. 统计关键词和情感
        Map<String, Integer> keywordFrequency = extractKeywords(reviewTexts);
        result.put("keywordFrequency", keywordFrequency);

        // 5. 提取菜品评价词
        Map<String, List<String>> dishAdjectives = extractDishAdjectives(reviewTexts, knownDishes);
        result.put("dishAdjectives", dishAdjectives);

        return result;
    }

    /**
     * 统计菜品提及次数
     */
    private Map<String, Integer> countDishMentions(List<String> reviewTexts, List<String> dishes) {
        Map<String, Integer> mentions = new HashMap<>();

        // 如果菜品列表为空，使用预设的通用菜品列表
        if (dishes == null || dishes.isEmpty()) {
            dishes = Arrays.asList(
                    "红烧肉", "宫保鸡丁", "水煮鱼", "麻婆豆腐",
                    "回锅肉", "糖醋排骨", "鱼香肉丝", "粉蒸肉"
            );
        }

        // 初始化
        for (String dish : dishes) {
            mentions.put(dish, 0);
        }

        // 统计提及次数
        for (String text : reviewTexts) {
            for (String dish : dishes) {
                if (text.contains(dish)) {
                    mentions.put(dish, mentions.get(dish) + 1);
                }
            }
        }

        // 过滤掉未提及的菜品
        return mentions.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 提取关键词及频率
     */
    private Map<String, Integer> extractKeywords(List<String> reviewTexts) {
        Map<String, Integer> keywordCounts = new HashMap<>();

        // 预定义关键词列表
        String[] serviceKeywords = {"服务", "态度", "上菜", "速度", "服务员", "热情", "冷淡", "周到"};
        String[] tasteKeywords = {"好吃", "难吃", "美味", "可口", "辣", "甜", "咸", "淡", "味道", "口感"};
        String[] environmentKeywords = {"环境", "装修", "整洁", "嘈杂", "安静", "舒适", "拥挤", "宽敞"};

        // 按类别统计关键词
        Map<String, String[]> categories = new HashMap<>();
        categories.put("服务", serviceKeywords);
        categories.put("口味", tasteKeywords);
        categories.put("环境", environmentKeywords);

        for (Map.Entry<String, String[]> category : categories.entrySet()) {
            for (String keyword : category.getValue()) {
                int count = 0;
                for (String text : reviewTexts) {
                    // 统计每个关键词在每条评论中的出现次数
                    Pattern pattern = Pattern.compile(keyword);
                    Matcher matcher = pattern.matcher(text);
                    while (matcher.find()) {
                        count++;
                    }
                }
                if (count > 0) {
                    keywordCounts.put(keyword, count);
                }
            }
        }

        return keywordCounts;
    }

    /**
     * 提取菜品形容词搭配
     */
    private Map<String, List<String>> extractDishAdjectives(List<String> reviewTexts, List<String> dishes) {
        Map<String, List<String>> dishAdjectives = new HashMap<>();

        // 初始化
        for (String dish : dishes) {
            dishAdjectives.put(dish, new ArrayList<>());
        }

        // 常见形容词
        String[] adjectives = {
                "好吃", "美味", "难吃", "一般", "辣", "咸", "甜", "新鲜",
                "推荐", "失望", "惊艳", "香", "正宗", "地道", "入味"
        };

        // 提取形容词搭配
        for (String text : reviewTexts) {
            for (String dish : dishes) {
                if (text.contains(dish)) {
                    int dishIndex = text.indexOf(dish);

                    for (String adj : adjectives) {
                        if (text.contains(adj)) {
                            int adjIndex = text.indexOf(adj);

                            // 如果形容词和菜品名称距离较近，视为搭配
                            if (Math.abs(dishIndex - adjIndex) < 15) {
                                dishAdjectives.get(dish).add(adj);
                            }
                        }
                    }
                }
            }
        }

        return dishAdjectives;
    }

    /**
     * 构建摘要提示词
     */
    private String buildSummaryPrompt(Map<String, Object> analysisData, List<Review> reviews) {
        StringBuilder prompt = new StringBuilder();

        // 系统指令
        prompt.append("你是一个专业的餐厅评论分析师，善于提取消费者评论中的关键信息。请根据以下数据生成一份简洁明了的餐厅评价摘要。\n\n");

        // 评分数据
        if (analysisData.containsKey("avgTasteScore")) {
            prompt.append("评分数据：\n");
            prompt.append("- 口味评分: ").append(analysisData.get("avgTasteScore")).append("\n");
            prompt.append("- 环境评分: ").append(analysisData.get("avgEnvironmentScore")).append("\n");
            prompt.append("- 服务评分: ").append(analysisData.get("avgServiceScore")).append("\n\n");
        }

        // 菜品提及统计
        if (analysisData.containsKey("dishMentions")) {
            Map<String, Integer> dishMentions = (Map<String, Integer>) analysisData.get("dishMentions");
            if (!dishMentions.isEmpty()) {
                prompt.append("菜品提及次数(共").append(reviews.size()).append("条评论)：\n");
                int count = 0;
                for (Map.Entry<String, Integer> entry : dishMentions.entrySet()) {
                    prompt.append("- ").append(entry.getKey()).append(": ")
                            .append(entry.getValue()).append("次\n");
                    if (++count >= 10) break;  // 最多显示10个菜品
                }
                prompt.append("\n");
            }
        }

        // 菜品形容词统计
        if (analysisData.containsKey("dishAdjectives")) {
            Map<String, List<String>> dishAdj = (Map<String, List<String>>) analysisData.get("dishAdjectives");
            prompt.append("菜品评价词：\n");
            for (Map.Entry<String, List<String>> entry : dishAdj.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    prompt.append("- ").append(entry.getKey()).append(": ");

                    // 统计形容词频率
                    Map<String, Long> adjFrequency = entry.getValue().stream()
                            .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

                    // 按频率排序
                    String adjText = adjFrequency.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .map(e -> e.getKey() + "(" + e.getValue() + "次)")
                            .collect(Collectors.joining(", "));

                    prompt.append(adjText).append("\n");
                }
            }
            prompt.append("\n");
        }

        // 关键词统计
        if (analysisData.containsKey("keywordFrequency")) {
            Map<String, Integer> keywords = (Map<String, Integer>) analysisData.get("keywordFrequency");
            prompt.append("关键词提及次数：\n");
            keywords.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(15)
                    .forEach(entry -> prompt.append("- ")
                            .append(entry.getKey()).append(": ")
                            .append(entry.getValue()).append("次\n"));
            prompt.append("\n");
        }

        // 部分原始评论样本
        int sampleSize = Math.min(reviews.size(), 5);
        if (sampleSize > 0) {
            prompt.append("评论样本：\n");
            for (int i = 0; i < sampleSize; i++) {
                prompt.append((i+1) + ". " + reviews.get(i).getContent() + "\n");
            }
            prompt.append("\n");
        }

        // 输出要求
        prompt.append("请生成一份200字左右的评论摘要，务必包含以下信息：\n");
        prompt.append("1. 最受欢迎的菜品\n");
        prompt.append("2. 菜品的口味特点（如偏辣、偏甜等）\n");
        prompt.append("3. 服务评价\n");
        prompt.append("4. 环境评价\n");
        prompt.append("5. 推荐菜品\n\n");
        prompt.append("注意：所有结论必须基于上述统计数据，请勿添加虚构内容。使用简洁客观的语言。");

        return prompt.toString();
    }

    /**
     * 保存或更新摘要
     */
    private void saveOrUpdateSummary(Long restaurantId, String summary,
                                     int reviewCount, Map<String, Object> analysisData) {
        ReviewSummary reviewSummary = reviewSummaryMapper.findByRestaurantId(restaurantId);

        if (reviewSummary == null) {
            reviewSummary = new ReviewSummary();
            reviewSummary.setRestaurantId(restaurantId);
        }

        reviewSummary.setSummary(summary);
        reviewSummary.setGeneratedAt(LocalDateTime.now());
        reviewSummary.setReviewCount(reviewCount);

        // 提取并保存常被提到的菜品
        if (analysisData.containsKey("dishMentions")) {
            Map<String, Integer> dishMentions = (Map<String, Integer>) analysisData.get("dishMentions");
            String mentionedDishes = dishMentions.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(","));

            reviewSummary.setMentionedDishes(mentionedDishes);
        }

        // 确定主要情感
        if (analysisData.containsKey("keywordFrequency")) {
            Map<String, Integer> keywords = (Map<String, Integer>) analysisData.get("keywordFrequency");

            // 简单情感判断
            int positiveCount = countKeywords(keywords, new String[]{"好吃", "美味", "推荐", "不错", "满意"});
            int negativeCount = countKeywords(keywords, new String[]{"难吃", "失望", "一般", "差", "不满"});

            String sentiment = positiveCount > negativeCount ? "正面" :
                    (negativeCount > positiveCount ? "负面" : "中性");

            reviewSummary.setMainSentiment(sentiment);
        }

        // 保存或更新
        if (reviewSummary.getId() == null) {
            reviewSummaryMapper.insert(reviewSummary);
        } else {
            reviewSummaryMapper.updateById(reviewSummary);
        }
    }

    /**
     * 统计特定关键词出现次数
     */
    private int countKeywords(Map<String, Integer> keywords, String[] targetWords) {
        int count = 0;
        for (String word : targetWords) {
            if (keywords.containsKey(word)) {
                count += keywords.get(word);
            }
        }
        return count;
    }
}