package com.foodmap.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodmap.entity.dto.ApiResponse;
import com.foodmap.entity.dto.ReviewCreateDTO;
import com.foodmap.entity.dto.ReviewDTO;
import com.foodmap.entity.dto.ReviewUpdateDTO;
import com.foodmap.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@Valid @RequestBody ReviewCreateDTO dto) {
        ReviewDTO createdReview = reviewService.createReview(dto);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateDTO dto) {
        ReviewDTO updatedReview = reviewService.updateReview(reviewId, dto);
        return ResponseEntity.ok(updatedReview);
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(new ApiResponse(true, "评论删除成功"));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<IPage<ReviewDTO>> getReviewsByRestaurant(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Page<ReviewDTO> page = new Page<>(current, size);
        IPage<ReviewDTO> reviews = reviewService.getReviewsByRestaurant(restaurantId, page);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<IPage<ReviewDTO>> getReviewsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Page<ReviewDTO> page = new Page<>(current, size);
        IPage<ReviewDTO> reviews = reviewService.getReviewsByUser(userId, page);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long reviewId) {
        ReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    @GetMapping("/user")
    public ResponseEntity<IPage<ReviewDTO>> getCurrentUserReviews(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Page<ReviewDTO> page = new Page<>(current, size);
        IPage<ReviewDTO> reviews = reviewService.getCurrentUserReviews(page);
        return ResponseEntity.ok(reviews);
    }
}