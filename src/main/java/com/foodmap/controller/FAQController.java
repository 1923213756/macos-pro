package com.foodmap.controller;

import com.foodmap.entity.dto.FAQRequest;
import com.foodmap.entity.dto.QuestionRequest;
import com.foodmap.entity.pojo.FAQ;
import com.foodmap.service.ai.FAQService;
import com.foodmap.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")  // 用于开发阶段，生产环境应限制来源
@Tag(name = "FAQ管理", description = "餐厅FAQ的创建、查询、更新与删除操作")
public class FAQController {

    private final FAQService faqService;

    /**
     * 获取餐厅的所有FAQ
     */
    @Operation(summary = "获取餐厅FAQ", description = "获取指定餐厅的所有常见问题解答")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取FAQ列表")
    })
    @GetMapping("/{restaurantId}")
    @PreAuthorize("hasRole('SHOP') or hasRole('USER')")
    public ResponseEntity<List<FAQ>> getFAQs(
            @Parameter(description = "餐厅ID", required = true)
            @PathVariable Long restaurantId) {
        List<FAQ> faqs = faqService.getFAQs(restaurantId);
        return ResponseEntity.ok(faqs);
    }

    /**
     * 添加新FAQ
     */
    @Operation(summary = "添加FAQ", description = "为餐厅添加新的FAQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "403", description = "无权添加FAQ")
    })
    @PostMapping
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<FAQ> addFAQ(@RequestBody FAQRequest request) {
        // 可以添加验证逻辑，确认当前用户是否为该餐厅的所有者
        FAQ faq = faqService.addFAQ(request.getRestaurantId(), request.getQuestion(), request.getAnswer());
        return ResponseEntity.ok(faq);
    }

    /**
     * 更新FAQ
     */
    @Operation(summary = "更新FAQ", description = "更新指定ID的FAQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "403", description = "无权更新此FAQ"),
            @ApiResponse(responseCode = "404", description = "指定ID的FAQ不存在")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<FAQ> updateFAQ(
            @Parameter(description = "FAQ ID", required = true)
            @PathVariable Long id,
            @RequestBody FAQRequest request) {
        FAQ faq = faqService.updateFAQ(id, request.getQuestion(), request.getAnswer());
        return ResponseEntity.ok(faq);
    }

    /**
     * 删除FAQ
     */
    @Operation(summary = "删除FAQ", description = "删除指定ID的FAQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "403", description = "无权删除此FAQ"),
            @ApiResponse(responseCode = "404", description = "指定ID的FAQ不存在")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<Void> deleteFAQ(
            @Parameter(description = "FAQ ID", required = true)
            @PathVariable Long id) {
        faqService.deleteFAQ(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 向FAQ系统提问 - 允许匿名访问
     */
    @Operation(summary = "向FAQ提问", description = "向指定餐厅的FAQ系统提问")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "返回回答成功")
    })
    @PostMapping("/ask")
    public ResponseEntity<Map<String, String>> askQuestion(@RequestBody QuestionRequest request) {
        // 如果没有提供会话ID，则创建一个新的
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        String answer = faqService.answerQuestion(
                1L, // 使用null作为userId，允许匿名访问
                request.getRestaurantId(),
                request.getQuestion(),
                sessionId
        );

        // 包装为JSON格式返回
        Map<String, String> response = new HashMap<>();
        response.put("answer", answer);
        return ResponseEntity.ok(response);
    }
    /**
     * 自动为餐厅生成FAQ - 使用WebClient实现
     */
    @Operation(summary = "自动生成FAQ", description = "为指定餐厅自动生成FAQ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "生成成功"),
            @ApiResponse(responseCode = "403", description = "无权为此餐厅生成FAQ")
    })
    @PostMapping("/auto-generate/{restaurantId}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseEntity<List<FAQ>> autoGenerateFAQs(
            @Parameter(description = "餐厅ID", required = true)
            @PathVariable Long restaurantId) {

        List<FAQ> generatedFAQs = faqService.autoGenerateFAQs(restaurantId);
        return ResponseEntity.ok(generatedFAQs);
    }
}