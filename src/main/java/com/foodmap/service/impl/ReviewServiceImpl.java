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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
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
        review.setEnvironmentScore(dto.getEnvironmentScore());
        review.setServiceScore(dto.getServiceScore());
        review.setTasteScore(dto.getTasteScore());

        // 4. 计算综合评分
        float avgRating = (dto.getEnvironmentScore() + dto.getServiceScore() + dto.getTasteScore()) / 3.0f;
        review.setCompositeScore(Math.round(avgRating));

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
        dto.setCompositeScore(review.getCompositeScore());
        dto.setEnvironmentScore(review.getEnvironmentScore());
        dto.setServiceScore(review.getServiceScore());
        dto.setTasteScore(review.getTasteScore());
        dto.setUserId(review.getUserId());
        dto.setRestaurantId(review.getRestaurantId());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setLikeCount(review.getLikeCount());
        // 设置其他必要字段...
        return dto;
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
        review.setCompositeScore(dto.getScore());

        reviewMapper.updateById(review);

        // 更新餐厅评分
        updateShopRatings(review.getRestaurantId());

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
        updateShopRatings(review.getRestaurantId());
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
    public IPage<ReviewDTO> getCurrentUserReviews(Page<ReviewDTO> page) {
        Long currentUserId = getCurrentUserId();
        return getReviewsByUser(currentUserId, page);
    }

    /**
     * 更新所有商铺的评分数据
     */
    @Override
    @Transactional
    public int updateAllShopsRatings() {
        int updatedCount = reviewMapper.updateAllShopsRatings();
        log.info("已更新{}家商铺的评分数据", updatedCount);
        return updatedCount;
    }

    /**
     * 更新指定商铺的评分数据
     */
    @Override
    @Transactional
    public boolean updateShopRatings(Long shopId) {
        if (shopId == null || shopId <= 0) {
            log.error("无效的商铺ID: {}", shopId);
            return false;
        }

        try {
            int result = reviewMapper.updateShopRatings(shopId);
            log.info("商铺ID {} 评分更新结果: {}", shopId, result > 0 ? "成功" : "无变化");
            return result > 0;
        } catch (Exception e) {
            log.error("更新商铺评分失败, 商铺ID: {}, 错误: {}", shopId, e.getMessage());
            return false;
        }
    }
}