package com.foodmap.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodmap.entity.dto.ReviewCreateDTO;
import com.foodmap.entity.dto.ReviewDTO;
import com.foodmap.entity.dto.ReviewUpdateDTO;
import com.foodmap.entity.pojo.Shop;
import com.foodmap.entity.pojo.Review;
import com.foodmap.entity.pojo.User;
import com.foodmap.exception.ResourceNotFoundException;
import com.foodmap.exception.UnauthorizedException;
import com.foodmap.mapper.ReviewMapper;
import com.foodmap.mapper.ShopMapper;
import com.foodmap.service.ReviewService;
import com.foodmap.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;
    private final ShopMapper shopMapper;
    private final UserService userService;

    @Override
    @Transactional
    public ReviewDTO createReview(ReviewCreateDTO dto) {
        // 1. 获取当前登录用户ID
        Long currentUserId = getCurrentUserId(); // 实现这个辅助方法

        // 2. 创建Review实体
        Review review = new Review();
        review.setContent(dto.getContent());
        review.setUserId(currentUserId);
        review.setRestaurantId(dto.getRestaurantId());

        // 3. 设置评分
        review.setEnvironmentRating(dto.getEnvironmentRating());
        review.setServiceRating(dto.getServiceRating());
        review.setTasteRating(dto.getTasteRating());

        // 4. 计算综合评分
        float avgRating = (dto.getEnvironmentRating() + dto.getServiceRating() + dto.getTasteRating()) / 3.0f;
        review.setRating(Math.round(avgRating));

        // 5. 设置默认值
        review.setLikeCount(0);
        review.setStatus(Review.STATUS_ACTIVE);

        // 6. 保存评论
        reviewMapper.insert(review);

        // 7. 返回结果
        return toReviewDTO(review);
    }

    // 获取当前用户ID的辅助方法
    private Long getCurrentUserId() {
        // 获取认证对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 通常认证主体是 UserDetails 的实现或用户名
        if (authentication != null && authentication.getPrincipal() != null) {
            String username = authentication.getName();
            return userService.getUserByName(username).getUserId();
        }

        throw new RuntimeException("用户未认证");
    }

    private ReviewDTO toReviewDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setContent(review.getContent());
        dto.setRating(review.getRating());
        dto.setEnvironmentRating(review.getEnvironmentRating());
        dto.setServiceRating(review.getServiceRating());
        dto.setTasteRating(review.getTasteRating());
        dto.setUserId(review.getUserId());
        dto.setRestaurantId(review.getRestaurantId());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setLikeCount(review.getLikeCount());
        // 设置其他必要字段...
        return dto;
    }


    private void updateShopRatings(Long shopId) {
        // 使用新方法获取所有维度的平均分
        Map<String, Double> ratings = reviewMapper.calculateAllRatings(shopId);
        Long reviewCount = reviewMapper.countActiveReviewsByRestaurant(shopId);

        // 更新商铺评分
        Shop shop = shopMapper.selectById(shopId);
        if (shop != null) {
            // 设置各个维度的评分
            shop.setCompositeScore(ratings.get("compositeScore") != null ?
                    ratings.get("compositeScore").floatValue() : 0f);
            shop.setEnvironmentScore(ratings.get("environmentScore") != null ?
                    ratings.get("environmentScore").floatValue() : 0f);
            shop.setServiceScore(ratings.get("serviceScore") != null ?
                    ratings.get("serviceScore").floatValue() : 0f);
            shop.setTasteScore(ratings.get("tasteScore") != null ?
                    ratings.get("tasteScore").floatValue() : 0f);

            // 设置评论数量
            shop.setReviewCount(reviewCount);

            // 更新数据库
            shopMapper.updateById(shop);
        }
    }

    @Override
    @Transactional
    public ReviewDTO updateReview(Long reviewId, ReviewUpdateDTO dto) {
        User currentUser = userService.getCurrentUser();

        Review review = reviewMapper.selectById(reviewId);
        if (review == null || Review.STATUS_DELETED.equals(review.getStatus())) {
            throw new ResourceNotFoundException("评论不存在");
        }

        if (!review.getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("您无权修改此评论");
        }

        review.setContent(dto.getContent());
        review.setRating(dto.getRating());

        reviewMapper.updateById(review);

        // 更新餐厅评分
        updateRestaurantRating(review.getRestaurantId());

        return reviewMapper.getReviewById(reviewId, currentUser.getUserId());
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        User currentUser = userService.getCurrentUser();

        Review review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new ResourceNotFoundException("评论不存在");
        }

        if (!review.getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("您无权删除此评论");
        }

        review.setStatus(Review.STATUS_DELETED);
        reviewMapper.updateById(review);

        // 更新餐厅评分
        updateRestaurantRating(review.getRestaurantId());
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<ReviewDTO> getReviewsByRestaurant(Long restaurantId, Page<ReviewDTO> page) {
        Shop shop= shopMapper.selectById(restaurantId);
        if (shop == null) {
            throw new ResourceNotFoundException("餐厅不存在");
        }

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        Long userId = currentUser != null ? currentUser.getUserId() : null;

        return reviewMapper.getReviewsByRestaurant(page, restaurantId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public IPage<ReviewDTO> getReviewsByUser(Long userId, Page<ReviewDTO> page) {
        // 检查用户是否存在
        if (userId == null) {
            throw new ResourceNotFoundException("用户不存在");
        }

        User currentUser = userService.getCurrentUserOptional().orElse(null);
        Long currentUserId = currentUser != null ? currentUser.getUserId() : null;

        return reviewMapper.getReviewsByUser(page, userId, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDTO getReviewById(Long reviewId) {
        User currentUser = userService.getCurrentUserOptional().orElse(null);
        Long userId = currentUser != null ? currentUser.getUserId() : null;

        ReviewDTO review = reviewMapper.getReviewById(reviewId, userId);
        if (review == null) {
            throw new ResourceNotFoundException("评论不存在或已被删除");
        }

        return review;
    }

    @Override
    @Transactional
    public void updateRestaurantRating(Long restaurantId) {
        Double avgRating = reviewMapper.calculateAverageRating(restaurantId);
        Long reviewCount = reviewMapper.countActiveReviewsByRestaurant(restaurantId);

        Shop shop= shopMapper.selectById(restaurantId);
        if (shop != null) {
            shop.setCompositeScore((float) (avgRating != null ? avgRating : 0.0));
            shop.setReviewCount(reviewCount);
            shopMapper.updateById(shop);
        }
    }
}