package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.ReviewSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

@Mapper
public interface ReviewSummaryMapper extends BaseMapper<ReviewSummary> {
    /**
     * 查询指定餐厅的评论摘要
     */
    @Select("SELECT * FROM review_summaries WHERE restaurant_id = #{restaurantId}")
    ReviewSummary findByRestaurantId(@Param("restaurantId") Long restaurantId);
}