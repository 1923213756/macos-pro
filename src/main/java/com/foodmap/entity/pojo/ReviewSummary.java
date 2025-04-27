package com.foodmap.entity.pojo;

import lombok.Data;

import java.util.Map;


@Data
public class ReviewSummary {
    private int totalReviews;
    private Map<String, Integer> keyPhrases;
    private Map<Integer, String> clusterSummaries;

}