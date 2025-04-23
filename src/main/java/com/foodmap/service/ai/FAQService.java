package com.foodmap.service.ai;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.foodmap.entity.pojo.ChatHistory;
import com.foodmap.entity.pojo.FAQ;
import com.foodmap.mapper.ChatHistoryMapper;
import com.foodmap.mapper.FAQMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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


    /**
     * 添加FAQ
     */
    public FAQ addFAQ(Long restaurantId, String question, String answer) {
        FAQ faq = new FAQ();
        faq.setRestaurantId(restaurantId);
        faq.setQuestion(question);
        faq.setAnswer(answer);

        // 自动提取关键词 (简化版)
        String keywords = extractKeywords(question);
        faq.setKeywords(keywords);

        faq.setCreatedAt(LocalDateTime.now());
        faq.setUpdatedAt(LocalDateTime.now());

        // 插入FAQ
        faqMapper.insert(faq);

        return faq;
    }

    /**
     * 简单的关键词提取方法
     */
    private String extractKeywords(String text) {
        // 简化版实现：去除停用词，保留主要词汇
        String[] stopWords = {"的", "了", "是", "在", "我", "有", "和", "与", "这", "那", "你", "您"};
        String result = text;

        for (String word : stopWords) {
            result = result.replace(word, " ");
        }

        // 分词并保留不重复的词
        Set<String> words = new HashSet<>();
        for (String word : result.split("\\s+|，|。|？|！")) {
            if (word.length() > 1) {  // 只保留长度>1的词
                words.add(word);
            }
        }

        return String.join(",", words);
    }

    /**
     * 删除FAQ
     */
    public void deleteFAQ(Long id) {
        faqMapper.deleteById(id);
    }

    /**
     * 简化版的RAG: 基于关键词检索回答用户问题
     */
    public String answerQuestion(Long userId, Long restaurantId, String question, String sessionId) {
        try {
            // 1. 提取问题中的关键词
            String keywords = extractKeywords(question);

            // 2. 按关键词匹配相关FAQ
            List<FAQ> relevantFAQs = new ArrayList<>();
            for (String keyword : keywords.split(",")) {
                if (keyword.length() > 1) {
                    List<FAQ> matches = faqMapper.searchByKeyword(restaurantId, keyword);
                    relevantFAQs.addAll(matches);
                }
            }

            // 去重
            relevantFAQs = relevantFAQs.stream()
                    .distinct()
                    .limit(3)  // 最多取前3条
                    .collect(Collectors.toList());

            // 3. 获取最近几轮对话历史
            Page<ChatHistory> page = new Page<>(1, 5);
            List<ChatHistory> recentChats = chatHistoryMapper.findRecentBySessionId(sessionId, page);
            Collections.reverse(recentChats); // 按时间正序排列

            // 4. 构建提示词
            String prompt = buildRAGPrompt(question, relevantFAQs, recentChats);

            // 5. 调用AI生成回答
            String answer = ollamaAiClient.generateText(prompt);

            // 6. 保存对话历史
            saveChat(userId, restaurantId, sessionId, question, answer);

            return answer;
        } catch (Exception e) {
            log.error("回答问题失败", e);
            return "抱歉，我无法回答这个问题。技术原因: " + e.getMessage();
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
}