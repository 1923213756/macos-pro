package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.ReviewGuide;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

@Mapper
public interface ReviewGuideMapper extends BaseMapper<ReviewGuide> {
    /**
     * 根据菜品名称查询评论引导
     */
    @Select("SELECT * FROM review_guides WHERE dish_name = #{dishName}")
    ReviewGuide findByDishName(@Param("dishName") String dishName);

    /**
     * 根据餐厅类型查询评论引导
     */
    @Select("SELECT * FROM review_guides WHERE restaurant_type = #{restaurantType}")
    ReviewGuide findByRestaurantType(@Param("restaurantType") String restaurantType);
}