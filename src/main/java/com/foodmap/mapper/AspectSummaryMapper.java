package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.AspectSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;

import java.util.List;
import java.util.Map;

@Mapper
public interface AspectSummaryMapper extends BaseMapper<AspectSummary> {

    /**
     * 获取餐厅的方面情感统计数据
     */
    @Select("SELECT * FROM aspect_summary WHERE restaurant_id = #{restaurantId}")
    List<AspectSummary> getAspectSummaryByRestaurant(@Param("restaurantId") Long restaurantId);

    /**
     * 获取餐厅的最新摘要信息
     */
    @Select("SELECT restaurant_id, GROUP_CONCAT(CONCAT(aspect, ':', ROUND(positive_percentage, 0), '%好评') SEPARATOR '，') AS summary " +
            "FROM aspect_summary WHERE restaurant_id = #{restaurantId} GROUP BY restaurant_id")
    Map<String, Object> getLatestSummaryText(@Param("restaurantId") Long restaurantId);

    /**
     * 批量更新或插入方面统计数据
     */
    int batchUpsertAspectStatistics(@Param("restaurantId") Long restaurantId,
                                    @Param("aspectStats") List<Map<String, Object>> aspectStats);

    int markReviewsAsAnalyzed(@Param("reviewIds") List<Long> reviewIds);

}