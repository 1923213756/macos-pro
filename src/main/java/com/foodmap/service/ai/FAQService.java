package com.foodmap.service.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodmap.entity.pojo.ChatHistory;
import com.foodmap.entity.pojo.FAQ;
import com.foodmap.mapper.ChatHistoryMapper;
import com.foodmap.mapper.FAQMapper;
import com.foodmap.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FAQService {

    private final FAQMapper faqMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final OllamaAiClient ollamaAiClient;
    private final ReviewMapper reviewMapper;
    private final WebClient webClient;

    /**
     * 自动为餐厅生成FAQ - 完全利用Python的聚类功能
     * @param restaurantId 餐厅ID
     * @return 生成的FAQ列表
     */
    public List<FAQ> autoGenerateFAQs(Long restaurantId) {
        try {
            log.info("开始为餐厅{}自动生成FAQ", restaurantId);

            // 1. 获取餐厅的评论数据
            List<String> reviews = reviewMapper.findReviewContentsByRestaurantId(restaurantId);

            if (reviews.isEmpty()) {
                log.warn("餐厅{}没有足够的评论来生成FAQ", restaurantId);
                return Collections.emptyList();
            }

            log.info("获取到{}条评论，准备调用Python聚类API", reviews.size());

            // 2. 调用Python API进行聚类分析
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("reviews", reviews);

            Map response = webClient.post()
                    .uri("/api/cluster")  // 使用专门的FAQ聚类API端点
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("Python API调用失败: " + errorBody)))
                    )
                    .bodyToMono(Map.class)
                    .block();

            // 3. 解析结果并保存到数据库
            if (response != null && response.containsKey("suggested_faqs")) {
                List<Map<String, Object>> suggestedFaqs = (List<Map<String, Object>>) response.get("suggested_faqs");

                log.info("成功获取Python API返回，包含{}个FAQ建议", suggestedFaqs.size());


                List<FAQ> savedFaqs = new ArrayList<>();
                for (Map<String, Object> faqData : suggestedFaqs) {
                    String question = (String) faqData.get("question");
                    String answer = (String) faqData.get("answer");

                    // 检查格式和内容
                    if (question == null || answer == null || question.isEmpty() || answer.isEmpty()) {
                        log.warn("跳过无效FAQ，问题或回答为空");
                        continue;
                    }

                    // 保存到数据库
                    FAQ faq = addFAQ(restaurantId, question, answer);
                    savedFaqs.add(faq);
                }

                log.info("成功为餐厅{}生成并保存了{}个FAQ", restaurantId, savedFaqs.size());
                return savedFaqs;
            } else {
                log.error("Python API返回异常: {}", response);
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("自动生成FAQ失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取指定餐厅的所有FAQ
     */
    public List<FAQ> getFAQs(Long restaurantId) {
        log.info("获取餐厅{}的FAQ列表", restaurantId);
        return faqMapper.findByRestaurantId(restaurantId);
    }

    /**
     * 添加FAQ
     */
    public FAQ addFAQ(Long restaurantId, String question, String answer) {
        log.info("为餐厅{}添加新FAQ: {}", restaurantId, question);

        FAQ faq = new FAQ();
        faq.setRestaurantId(restaurantId);
        faq.setQuestion(question);
        faq.setAnswer(answer);

        // 自动提取关键词
        String keywords = extractKeywords(question);
        faq.setKeywords(keywords);

        faq.setCreatedAt(LocalDateTime.now());
        faq.setUpdatedAt(LocalDateTime.now());

        // 插入FAQ
        faqMapper.insert(faq);
        log.info("成功添加FAQ，ID: {}", faq.getId());

        return faq;
    }

    /**
     * 更新FAQ
     */
    public FAQ updateFAQ(Long id, String question, String answer) {
        log.info("更新FAQ，ID: {}", id);

        FAQ faq = faqMapper.selectById(id);
        if (faq == null) {
            log.error("FAQ不存在，ID: {}", id);
            throw new RuntimeException("FAQ不存在，ID: " + id);
        }

        // 更新问题和回答
        faq.setQuestion(question);
        faq.setAnswer(answer);

        // 重新提取关键词
        String keywords = extractKeywords(question);
        faq.setKeywords(keywords);

        faq.setUpdatedAt(LocalDateTime.now());

        // 更新数据库
        faqMapper.updateById(faq);
        log.info("FAQ更新成功，ID: {}", id);

        return faq;
    }

    /**
     * 删除FAQ
     */
    public void deleteFAQ(Long id) {
        log.info("删除FAQ，ID: {}", id);
        faqMapper.deleteById(id);
    }

    /**
     * 从问题中提取关键词
     * 简单版本，直接使用分词并保留重要词汇
     */
    private String extractKeywords(String question) {
        // 使用Ollama或Python API提取关键词
        try {
            String prompt = "请从以下餐厅FAQ问题中提取3-5个关键词，仅返回关键词，用逗号分隔：\n" + question;

            // 调用Ollama客户端生成文本
            String response = ollamaAiClient.generateText(prompt);

            // 处理响应
            if (response != null && !response.isEmpty()) {
                return response.trim();
            }
        } catch (Exception e) {
            log.warn("提取关键词失败，使用简单提取: {}", e.getMessage());
        }

        // 如果API调用失败，使用简单提取方法
        return question.replaceAll("[\\p{P}\\s]", ",").replaceAll(",+", ",");
    }

    /**
     * 回答用户问题 - 基于现有FAQ匹配
     */
    public String answerQuestion(Long userId, Long restaurantId, String question, String sessionId) {
        try {
            log.info("处理用户问题，餐厅ID: {}, 问题: {}", restaurantId, question);

            // 1. 获取餐厅的所有FAQ
            List<FAQ> faqs = faqMapper.findByRestaurantId(restaurantId);
            if (faqs.isEmpty()) {
                log.info("餐厅{}没有现有FAQ，将使用通用回答", restaurantId);
                return generateGenericAnswer(question);
            }

            log.info("已获取餐厅{}的{}个FAQ记录", restaurantId, faqs.size());

            // 2. 转换为Python API所需的格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("question", question);

            List<Map<String, Object>> existingFaqs = new ArrayList<>();
            for (FAQ faq : faqs) {
                Map<String, Object> faqMap = new HashMap<>();
                faqMap.put("id", faq.getId());
                faqMap.put("question", faq.getQuestion());
                faqMap.put("answer", faq.getAnswer());
                existingFaqs.add(faqMap);
            }
            requestBody.put("existing_faqs", existingFaqs);

            // 3. 调用Python API进行匹配
            log.info("调用FAQ匹配API");
            Map response = webClient.post()
                    .uri("/api/match_faq")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("API调用失败: " + errorBody)))
                    )
                    .bodyToMono(Map.class)
                    .block();

            // 4. 处理匹配结果
            if (response != null && response.containsKey("matched_faqs")) {
                List<Map<String, Object>> matchedFaqs = (List<Map<String, Object>>) response.get("matched_faqs");

                if (!matchedFaqs.isEmpty()) {
                    // 如果找到匹配的FAQ，使用匹配结果
                    Map<String, Object> bestMatch = matchedFaqs.get(0);
                    double similarity = (double) bestMatch.get("similarity");
                    String matchedQuestion = (String) bestMatch.get("question");

                    log.info("找到FAQ匹配，最佳匹配相似度: {}, 问题: {}", similarity, matchedQuestion);

                    // 如果相似度足够高，直接返回答案
                    if (similarity > 0.75) {
                        String answer = (String) bestMatch.get("answer");
                        log.info("相似度超过阈值，直接返回匹配的答案");

                        // 保存聊天记录
                        saveChat(userId, restaurantId, sessionId, question, answer);

                        return answer;
                    }
                }
            }

            // 5. 如果没有找到匹配的FAQ或相似度不够，使用RAG生成回答
            log.info("未找到足够相似的FAQ，使用RAG生成回答");
            return generateAnswerUsingRAG(userId, restaurantId, sessionId, question, faqs);

        } catch (Exception e) {
            log.error("FAQ匹配失败", e);
            return "很抱歉，系统暂时无法处理您的问题，请稍后再试。";
        }
    }

    /**
     * 使用RAG方法生成回答
     */
    private String generateAnswerUsingRAG(Long userId, Long restaurantId, String sessionId, String question, List<FAQ> allFaqs) {
        try {
            // 获取与问题相关的FAQ作为上下文（而不是使用所有FAQ）
            List<FAQ> relevantFAQs = searchRelevantFAQs(restaurantId, question);
            log.info("为问题【{}】找到{}个相关FAQ作为上下文", question, relevantFAQs.size());
            
            // 获取聊天历史
            List<ChatHistory> chatHistory = getChatHistory(userId, restaurantId, sessionId, 5);

            // 构建RAG提示词
            String prompt = buildRAGPrompt(question, relevantFAQs, chatHistory);
            
            // 调用AI生成回答
            log.info("使用RAG方法生成问题【{}】的回答", question);
            String answer = ollamaAiClient.generateText(prompt);

            // 如果生成失败，返回回退回答
            if (answer == null || answer.isEmpty()) {
                answer = "非常抱歉，我暂时无法回答这个问题。您可以询问其他问题，或者稍后再试。";
                log.warn("RAG回答生成失败，使用默认回答");
            } else {
                // 将高质量的RAG回答保存为新的FAQ
                saveFAQFromRAGAnswer(restaurantId, question, answer);
            }

            // 保存聊天记录
            saveChat(userId, restaurantId, sessionId, question, answer);

            return answer;
        } catch (Exception e) {
            log.error("RAG生成回答失败", e);
            String fallbackAnswer = "非常抱歉，系统暂时出现问题，无法回答您的问题。";
            saveChat(userId, restaurantId, sessionId, question, fallbackAnswer);
            return fallbackAnswer;
        }
    }

    /**
     * 搜索相关FAQ
     */
    private List<FAQ> searchRelevantFAQs(Long restaurantId, String question) {
        // 1. 先使用关键词搜索
        List<String> keywords = Arrays.asList(extractKeywords(question).split(","));
        List<FAQ> results = new ArrayList<>();

        for (String keyword : keywords) {
            if (keyword.trim().isEmpty()) continue;

            List<FAQ> matched = faqMapper.searchByKeyword(restaurantId, keyword.trim());
            for (FAQ faq : matched) {
                if (!results.contains(faq)) {
                    results.add(faq);
                }
            }

            // 限制结果数量
            if (results.size() >= 5) break;
        }

        // 2. 如果关键词搜索结果不足，添加一些默认FAQ
        if (results.size() < 3) {
            List<FAQ> defaultFAQs = faqMapper.findTopByRestaurantId(restaurantId, 3 - results.size());
            for (FAQ faq : defaultFAQs) {
                if (!results.contains(faq)) {
                    results.add(faq);
                }
            }
        }

        return results;
    }

    /**
     * 获取最近的聊天历史
     */
    private List<ChatHistory> getChatHistory(Long userId, Long restaurantId, String sessionId, int limit) {
        if (userId == null) {
            // 匿名用户仅使用会话ID
            return chatHistoryMapper.findBySessionId(sessionId, limit);
        } else {
            // 登录用户使用用户ID和餐厅ID
            return chatHistoryMapper.findByUserIdAndRestaurantId(userId, restaurantId, limit);
        }
    }

    /**
     * 构建RAG提示词
     */
    private String buildRAGPrompt(String question, List<FAQ> relevantFAQs, List<ChatHistory> chatHistory) {
        StringBuilder prompt = new StringBuilder();

        // 1. 系统指令
        prompt.append("你是一个餐厅客服AI助手。请根据提供的知识库信息和对话历史来回答用户的问题。\n\n");

        // 2. 添加知识库内容
        if (!relevantFAQs.isEmpty()) {
            prompt.append("知识库信息：\n");
            for (int i = 0; i < relevantFAQs.size(); i++) {
                FAQ faq = relevantFAQs.get(i);
                prompt.append("问题").append(i+1).append(": ").append(faq.getQuestion()).append("\n");
                prompt.append("回答").append(i+1).append(": ").append(faq.getAnswer()).append("\n\n");
            }
        } else {
            prompt.append("知识库中没有相关信息。\n\n");
        }

        // 3. 添加对话历史
        if (!chatHistory.isEmpty()) {
            prompt.append("最近的对话历史：\n");
            for (ChatHistory chat : chatHistory) {
                String role = chat.getIsUserMessage() ? "用户" : "助手";
                prompt.append(role).append(": ").append(chat.getContent()).append("\n");
            }
            prompt.append("\n");
        }

        // 4. 添加当前问题和回答要求
        prompt.append("当前用户问题: ").append(question).append("\n\n");
        prompt.append("请根据以上信息回答用户问题。如果知识库中有相关信息，请基于这些信息回答。");
        prompt.append("如果问题超出了知识库范围，请礼貌地告知用户你无法回答，并建议用户咨询其他相关问题。");
        prompt.append("回答应简洁、准确、有礼貌。");

        return prompt.toString();
    }

    /**
     * 生成通用回答
     * 当餐厅没有FAQ数据时使用
     */
    private String generateGenericAnswer(String question) {
        log.info("生成通用回答，问题: {}", question);

        try {
            // 构建提示词
            String prompt = "你是一个餐厅客服AI助手。用户问了一个问题，但我们没有特定的FAQ来回答。\n\n" +
                    "请生成一个友好、专业的回复，告知用户我们目前没有相关信息，并鼓励他们询问其他问题。\n\n" +
                    "用户问题: " + question + "\n\n" +
                    "回复应该简短、礼貌，不要编造信息。";

            // 调用AI生成回答
            String answer = ollamaAiClient.generateText(prompt);

            // 如果生成失败，返回默认回答
            if (answer == null || answer.isEmpty()) {
                return "非常感谢您的提问。目前我们没有关于这个问题的特定信息，建议您直接联系餐厅了解详情。您还有其他问题吗？";
            }

            return answer;
        } catch (Exception e) {
            log.error("生成通用回答失败", e);
            return "非常抱歉，我暂时无法回答这个问题。您可以询问其他问题，或者稍后再试。";
        }
    }

    /**
     * 保存聊天记录
     */
    private void saveChat(Long userId, Long restaurantId, String sessionId, String question, String answer) {
        // 保存用户问题
        ChatHistory userChat = new ChatHistory();
        userChat.setUserId(userId);
        userChat.setRestaurantId(restaurantId);
        userChat.setSessionId(sessionId);
        userChat.setIsUserMessage(true);
        userChat.setContent(question);
        userChat.setCreatedAt(LocalDateTime.now());
        chatHistoryMapper.insert(userChat);

        // 保存AI回答
        ChatHistory aiChat = new ChatHistory();
        aiChat.setUserId(userId);
        aiChat.setRestaurantId(restaurantId);
        aiChat.setSessionId(sessionId);
        aiChat.setIsUserMessage(false);
        aiChat.setContent(answer);
        aiChat.setCreatedAt(LocalDateTime.now());
        chatHistoryMapper.insert(aiChat);
    }

    /**
     * 保存高质量的RAG回答为新的FAQ
     * @param restaurantId 餐厅ID
     * @param question 用户问题
     * @param answer 生成的回答
     */
    private void saveFAQFromRAGAnswer(Long restaurantId, String question, String answer) {
        try {
            log.info("尝试将RAG生成的回答保存为FAQ - 问题: {}", question);
            
            // 2. 检查是否已存在相似问题
            List<FAQ> existingFaqs = faqMapper.findByRestaurantId(restaurantId);
            
            // 3. 转换为Python API所需的格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("question", question);

            List<Map<String, Object>> faqList = new ArrayList<>();
            for (FAQ faq : existingFaqs) {
                Map<String, Object> faqMap = new HashMap<>();
                faqMap.put("id", faq.getId());
                faqMap.put("question", faq.getQuestion());
                faqMap.put("answer", faq.getAnswer());
                faqList.add(faqMap);
            }
            requestBody.put("existing_faqs", faqList);
            
            // 4. 调用Python API检查相似度
            log.info("检查问题【{}】与现有FAQ的相似度", question);
            Map response = webClient.post()
                    .uri("/api/match_faq")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .flatMap(errorBody -> Mono.error(new RuntimeException("API调用失败: " + errorBody)))
                    )
                    .bodyToMono(Map.class)
                    .block();
            
            // 判断是否保存为新FAQ (阈值设置较高以避免重复)
            if (response != null && response.containsKey("matched_faqs")) {
                List<Map<String, Object>> matchedFaqs = (List<Map<String, Object>>) response.get("matched_faqs");
                
                if (!matchedFaqs.isEmpty()) {
                    // 找到最佳匹配
                    Map<String, Object> bestMatch = matchedFaqs.get(0);
                    double similarity = (double) bestMatch.get("similarity");
                    
                    // 如果存在高相似度FAQ，不再保存
                    if (similarity > 0.85) {
                        log.info("已存在相似问题，相似度: {}, 不保存为新FAQ", similarity);
                        return;
                    }
                }
            }
            
            // 5. 创建并保存新FAQ
            FAQ newFaq = new FAQ();
            newFaq.setRestaurantId(restaurantId);
            newFaq.setQuestion(question);
            newFaq.setAnswer(answer);
            
            // 自动提取关键词
            String keywords = extractKeywords(question);
            newFaq.setKeywords(keywords);
            
            // 添加自动生成标记和时间戳
            newFaq.setCreatedAt(LocalDateTime.now());
            newFaq.setUpdatedAt(LocalDateTime.now());
            
            faqMapper.insert(newFaq);
            log.info("成功将RAG回答保存为新FAQ，ID: {}", newFaq.getId());
        } catch (Exception e) {
            log.error("保存RAG答案为FAQ失败: {}", e.getMessage());
        }
    }

}