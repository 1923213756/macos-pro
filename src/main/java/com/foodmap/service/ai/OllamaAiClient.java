package com.foodmap.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class OllamaAiClient {

    private final RestTemplate restTemplate;
    private final String ollamaEndpoint;
    private final String model;

    public OllamaAiClient(
            @Value("${ollama.api.endpoint:http://localhost:11434}") String ollamaEndpoint,
            @Value("${ollama.model:llama3.2:latest}") String model) {
        this.restTemplate = new RestTemplate();
        this.ollamaEndpoint = ollamaEndpoint;
        this.model = model;

        // 输出配置信息，便于调试
        System.out.println("Ollama AI Client initialized with endpoint: " + ollamaEndpoint);
        System.out.println("Using model: " + model);
    }

    public String generateText(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求体 - 基于Ollama API格式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);  // 不使用流式响应
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // 发送请求到Ollama API
            Map<String, Object> response = restTemplate.postForObject(
                    ollamaEndpoint + "/api/generate",
                    request,
                    Map.class
            );

            // 解析Ollama响应
            if (response != null && response.containsKey("response")) {
                return (String) response.get("response");
            }

            return "无法生成回复，请检查Ollama服务";
        } catch (Exception e) {
            e.printStackTrace();
            return "连接Ollama服务失败: " + e.getMessage();
        }
    }
}