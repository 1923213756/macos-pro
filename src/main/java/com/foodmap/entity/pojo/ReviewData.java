package com.foodmap.entity.pojo;

import java.util.List;

public class ReviewData {
    private List<String> reviews;

    // 构造函数、getter和setter
    public ReviewData() {}

    public ReviewData(List<String> reviews) {
        this.reviews = reviews;
    }

    public List<String> getReviews() {
        return reviews;
    }

    public void setReviews(List<String> reviews) {
        this.reviews = reviews;
    }
}