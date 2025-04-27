package com.foodmap.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Component
public class PythonNlpProcessor {
    private final String pythonScript;
    private final ObjectMapper objectMapper;

    public PythonNlpProcessor(
            @Value("${python.script.path:python/nlp_processor.py}") String pythonScriptPath) {
        this.pythonScript = pythonScriptPath;
        this.objectMapper = new ObjectMapper();

        System.out.println("PythonNlpProcessor initialized with script: " + pythonScriptPath);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> processReviews(List<String> reviews, int numClusters) {
        try {
            // 准备命令
            ProcessBuilder pb = new ProcessBuilder(
                    getPythonCommand(),
                    pythonScript,
                    "--clusters", String.valueOf(numClusters)
            );

            // 启动进程
            Process process = pb.start();

            // 写入评论数据
            try (OutputStream outputStream = process.getOutputStream()) {
                objectMapper.writeValue(outputStream, reviews);
            }

            // 读取处理结果
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            // 读取错误输出（用于调试）
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            if (errorOutput.length() > 0) {
                System.err.println("Python script error output: " + errorOutput);
            }

            // 等待进程完成
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python处理失败，退出代码: " + exitCode + "\n错误信息: " + errorOutput);
            }

            // 解析结果
            return objectMapper.readValue(output.toString(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("NLP处理失败: " + e.getMessage(), e);
        }
    }

    private String getPythonCommand() {
        // 根据操作系统确定Python命令
        return SystemUtils.IS_OS_WINDOWS ? "python" : "python3";
    }
}