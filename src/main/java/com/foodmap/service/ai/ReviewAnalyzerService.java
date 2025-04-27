package com.foodmap.service.ai;

import com.foodmap.entity.pojo.ReviewData;
import com.foodmap.entity.pojo.ReviewSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewAnalyzerService {
    private final PythonNlpProcessor nlpProcessor;
    private final OllamaAiClient ollamaAiClient;
    private int defaultClusters = 5;



    public ReviewSummary analyzeReviews(ReviewData data) {
        return analyzeReviews(data.getReviews(), defaultClusters);
    }

    public ReviewSummary analyzeReviews(List<String> reviews, int numClusters) {
        // 1. 使用Python处理评论数据
        Map<String, Object> processingResult = nlpProcessor.processReviews(reviews, numClusters);

        // 2. 提取处理结果
        @SuppressWarnings("unchecked")
        Map<String, Integer> keyPhrases = (Map<String, Integer>) processingResult.get("key_phrases");

        @SuppressWarnings("unchecked")
        Map<String, Object> clusterStats = (Map<String, Object>) processingResult.get("cluster_stats");

        // 3. 使用LLM生成摘要
        Map<Integer, String> summaries = generateClusterSummaries(clusterStats);

        // 4. 构建结果
        ReviewSummary summary = new ReviewSummary();
        summary.setTotalReviews(reviews.size());
        summary.setKeyPhrases(keyPhrases);
        summary.setClusterSummaries(summaries);

        return summary;
    }

    private Map<Integer, String> generateClusterSummaries(Map<String, Object> clusterStats) {
        Map<Integer, String> summaries = new HashMap<>();

        for (Map.Entry<String, Object> entry : clusterStats.entrySet()) {
            int clusterId = Integer.parseInt(entry.getKey());
            @SuppressWarnings("unchecked")
            Map<String, Object> stats = (Map<String, Object>) entry.getValue();

            // 构建提示
            String prompt = buildPromptForCluster(stats);

            // 调用Ollama生成摘要 - 使用现有OllamaAiClient的方法
            String summary = ollamaAiClient.generateText(prompt);
            summaries.put(clusterId, summary.trim());
        }

        return summaries;
    }

    private String buildPromptForCluster(Map<String, Object> stats) {
        @SuppressWarnings("unchecked")
        List<List<Object>> topWords = (List<List<Object>>) stats.get("top_words");
        int count = (Integer) stats.get("count");

        @SuppressWarnings("unchecked")
        List<String> sampleComments = (List<String>) stats.get("sample_comments");

        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下信息生成简洁的评论摘要:\n\n");
        prompt.append("评论数量: ").append(count).append("\n");
        prompt.append("高频词语: ");

        // 格式化高频词
        for (int i = 0; i < Math.min(topWords.size(), 10); i++) {
            List<Object> wordCount = topWords.get(i);
            prompt.append(wordCount.get(0)).append("(").append(wordCount.get(1)).append("次)");
            if (i < Math.min(topWords.size(), 10) - 1) {
                prompt.append(", ");
            }
        }
        prompt.append("\n\n示例评论:\n");

        // 添加示例评论
        for (int i = 0; i < Math.min(sampleComments.size(), 5); i++) {
            prompt.append("- ").append(sampleComments.get(i)).append("\n");
        }

        prompt.append("\n根据上述统计数据(尤其是词频统计)，生成1-3条简短的摘要信息，格式为\"特点:次数\"，例如\"口味偏辣:15条\"。\n");
        prompt.append("不要编造数据，只使用提供的统计信息。\n");

        return prompt.toString();
    }
}