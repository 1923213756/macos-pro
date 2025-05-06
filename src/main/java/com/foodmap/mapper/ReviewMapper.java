package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.foodmap.entity.dto.ReviewDTO;
import com.foodmap.entity.pojo.Review;
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


    // 新增方法 - 获取尚未分析的评论
    @Select("SELECT id, content FROM reviews WHERE sentiment_analyzed = 0 AND status = 1 LIMIT #{limit}")
    List<Map<String, Object>> findUnanalyzedReviews(@Param("limit") int limit);

    // 新增方法 - 按餐厅ID获取尚未分析的评论
    @Select("SELECT id, content FROM reviews WHERE sentiment_analyzed = 0 AND status = 1 AND restaurantId = #{restaurantId} LIMIT #{limit}")
    List<Map<String, Object>> findUnanalyzedReviewsByRestaurant(@Param("restaurantId") Long restaurantId, @Param("limit") int limit);

    // 新增方法 - 标记评论已分析
    @Update("UPDATE reviews SET sentiment_analyzed = 1, analyzed_at = NOW() WHERE id IN (${reviewIds})")
    int markReviewsAsAnalyzed(@Param("reviewIds") String reviewIds);

    // 新增方法 - 重置评论的分析状态（用于重新分析）
    @Update("UPDATE reviews SET sentiment_analyzed = 0, analyzed_at = NULL WHERE restaurantId = #{restaurantId}")
    int resetAnalysisStatusByRestaurant(@Param("restaurantId") Long restaurantId);

    // 新增方法 - 获取最近的评论用于分析
    @Select("SELECT id, content FROM reviews WHERE status = 1 AND restaurantId = #{restaurantId} " +
            "ORDER BY createdAt DESC LIMIT #{limit}")
    List<Map<String, Object>> getRecentReviewsForAnalysis(@Param("restaurantId") Long restaurantId, @Param("limit") int limit);
}
