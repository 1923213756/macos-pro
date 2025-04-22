package com.foodmap.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodmap.entity.dto.ReviewCreateDTO;
import com.foodmap.entity.dto.ReviewDTO;
import com.foodmap.entity.dto.ReviewUpdateDTO;

public interface ReviewService {

    /**
     * 创建评论
     */
    ReviewDTO createReview(ReviewCreateDTO dto);

    /**
     * 更新评论
     */
    ReviewDTO updateReview(Long reviewId, ReviewUpdateDTO dto);

    /**
     * 删除评论
     */
    void deleteReview(Long reviewId);

    /**
     * 获取餐厅的所有评论
     */
    IPage<ReviewDTO> getReviewsByRestaurant(Long restaurantId, Page<ReviewDTO> page);

    /**
     * 获取用户的所有评论
     */
    IPage<ReviewDTO> getReviewsByUser(Long userId, Page<ReviewDTO> page);

    /**
     * 获取评论详情
     */
    ReviewDTO getReviewById(Long reviewId);


    IPage<ReviewDTO> getCurrentUserReviews(Page<ReviewDTO> page);

    /**
     * 更新所有商铺的评分数据
     * @return 更新的商铺数量
     */
    int updateAllShopsRatings();

    /**
     * 更新指定商铺的评分数据
     * @param shopId 商铺ID
     * @return 是否更新成功
     */
    boolean updateShopRatings(Long shopId);
}