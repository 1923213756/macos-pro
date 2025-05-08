package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.FAQ;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface FAQMapper extends BaseMapper<FAQ> {
    /**
     * 查询指定餐厅的所有FAQ
     */
    @Select("SELECT * FROM faqs WHERE restaurantId = #{restaurantId}")
    List<FAQ> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * 根据关键词搜索FAQ
     * 在问题内容或预定义关键词中匹配关键词
     */
    @Select("SELECT * FROM faqs WHERE restaurantId = #{restaurantId} AND " +
            "(question LIKE CONCAT('%', #{keyword}, '%') OR " +
            "keywords LIKE CONCAT('%', #{keyword}, '%'))")
    List<FAQ> searchByKeyword(@Param("restaurantId") Long restaurantId,
                              @Param("keyword") String keyword);

    /**
     * 查询餐厅排名靠前的FAQ
     */
    @Select("SELECT * FROM faqs WHERE restaurantId = #{restaurantId} ORDER BY id DESC LIMIT #{limit}")
    List<FAQ> findTopByRestaurantId(@Param("restaurantId") Long restaurantId, @Param("limit") int limit);
}