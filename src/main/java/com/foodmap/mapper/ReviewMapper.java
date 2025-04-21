package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodmap.entity.pojo.Review;
import com.foodmap.entity.dto.ReviewDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    // 计算综合评分（所有维度的平均分）
    @Select("SELECT AVG(rating) FROM reviews WHERE restaurant_id = #{restaurantId} AND status = 'ACTIVE'")
    Double calculateAverageRating(@Param("restaurantId") Long restaurantId);

    // 计算环境评分
    @Select("SELECT AVG(environment_rating) FROM reviews WHERE restaurant_id = #{restaurantId} AND status = 'ACTIVE'")
    Double calculateEnvironmentRating(@Param("restaurantId") Long restaurantId);

    // 计算服务评分
    @Select("SELECT AVG(service_rating) FROM reviews WHERE restaurant_id = #{restaurantId} AND status = 'ACTIVE'")
    Double calculateServiceRating(@Param("restaurantId") Long restaurantId);

    // 计算口味评分
    @Select("SELECT AVG(taste_rating) FROM reviews WHERE restaurant_id = #{restaurantId} AND status = 'ACTIVE'")
    Double calculateTasteRating(@Param("restaurantId") Long restaurantId);

    // 计算所有维度评分（整合方法）
    @Select("SELECT " +
            "AVG(rating) as compositeScore, " +
            "AVG(environment_rating) as environmentScore, " +
            "AVG(service_rating) as serviceScore, " +
            "AVG(taste_rating) as tasteScore " +
            "FROM reviews WHERE restaurant_id = #{restaurantId} AND status = 'ACTIVE'")
    Map<String, Double> calculateAllRatings(@Param("restaurantId") Long restaurantId);

    // 统计评论数
    @Select("SELECT COUNT(*) FROM reviews WHERE restaurant_id = #{restaurantId} AND status = 'ACTIVE'")
    Long countActiveReviewsByRestaurant(@Param("restaurantId") Long restaurantId);

    // 下面的方法可能需要在对应的XML文件中实现，以支持复杂查询
    IPage<ReviewDTO> getReviewsByRestaurant(Page<ReviewDTO> page, @Param("restaurantId") Long restaurantId, @Param("userId") Long currentUserId);

    IPage<ReviewDTO> getReviewsByUser(Page<ReviewDTO> page, @Param("userId") Long userId, @Param("currentUserId") Long currentUserId);

    ReviewDTO getReviewById(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
}