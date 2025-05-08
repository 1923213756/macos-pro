package com.foodmap.entity.dto;

import lombok.Data;

@Data
public class QuestionRequest {
    private Long restaurantId;
    private String question;
    private String sessionId;
}