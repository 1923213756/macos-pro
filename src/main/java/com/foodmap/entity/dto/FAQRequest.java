package com.foodmap.entity.dto;

import lombok.Data;

@Data
public class FAQRequest {
    private Long restaurantId;
    private String question;
    private String answer;
}