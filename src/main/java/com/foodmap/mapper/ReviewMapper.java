package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodmap.entity.pojo.Review;
import com.foodmap.entity.dto.ReviewDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    // 计算综合评分（所有维度的平均分）
    Double calculateAverageRating(@Param("restaurantId") Long restaurantId);

    // 统计评论数
    Long countActiveReviewsByRestaurant(@Param("restaurantId") Long restaurantId);

    // 下面的方法可能需要在对应的XML文件中实现，以支持复杂查询
    IPage<ReviewDTO> getReviewsByRestaurant(Page<ReviewDTO> page, @Param("restaurantId") Long restaurantId, @Param("userId") Long currentUserId);

    IPage<ReviewDTO> getReviewsByUser(Page<ReviewDTO> page, @Param("userId") Long userId, @Param("currentUserId") Long currentUserId);

    ReviewDTO getReviewById(@Param("reviewId") Long reviewId, @Param("userId") Long userId);

    int updateAllShopsRatings();

    int updateShopRatings(@Param("shopId") Long shopId);
    
    long countByRestaurantId(Long restaurantId);

    List<Review> findRecentByRestaurantId(Long restaurantId, Page<Review> page);
}