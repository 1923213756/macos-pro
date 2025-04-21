package com.foodmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodmap.entity.dto.LikeCreateDTO;
import com.foodmap.entity.pojo.Like;
import com.foodmap.entity.pojo.Review;
import com.foodmap.entity.pojo.User;
import com.foodmap.exception.ResourceNotFoundException;
import com.foodmap.mapper.LikeMapper;
import com.foodmap.mapper.ReviewMapper;
import com.foodmap.service.LikeService;
import com.foodmap.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeMapper likeMapper;
    private final ReviewMapper reviewMapper;
    private final UserService userService;

    @Override
    @Transactional
    public boolean toggleLike(LikeCreateDTO dto) {
        User currentUser = userService.getCurrentUser();

        Review review = reviewMapper.selectById(dto.getReviewId());
        if (review == null || Review.STATUS_DELETED.equals(review.getStatus())) {
            throw new ResourceNotFoundException("评论不存在");
        }

        LambdaQueryWrapper<Like> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Like::getUserId, currentUser.getUserId())
                .eq(Like::getReviewId, dto.getReviewId());

        Like existingLike = likeMapper.selectOne(queryWrapper);

        if (existingLike != null) {
            // 已点赞，则取消点赞
            likeMapper.deleteById(existingLike.getId());

            // 更新点赞数
            updateLikeCount(review, false);
            return false;
        } else {
            // 未点赞，则添加点赞
            Like like = Like.builder()
                    .userId(currentUser.getUserId())
                    .reviewId(dto.getReviewId())
                    .type(dto.getType())
                    .build();

            likeMapper.insert(like);

            // 更新点赞数
            updateLikeCount(review, true);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserLiked(Long reviewId) {
        User currentUser = userService.getCurrentUser();

        Integer count = likeMapper.checkUserLiked(currentUser.getUserId(), reviewId);
        return count != null && count > 0;
    }

    private void updateLikeCount(Review review, boolean isIncrease) {
        LambdaUpdateWrapper<Review> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Review::getId, review.getId());

        if (isIncrease) {
            updateWrapper.setSql("like_count = like_count + 1");
        } else {
            updateWrapper.setSql("like_count = GREATEST(0, like_count - 1)");
        }

        reviewMapper.update(null, updateWrapper);
    }
}