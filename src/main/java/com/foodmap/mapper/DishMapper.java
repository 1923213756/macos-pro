package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.Dish;
import io.lettuce.core.dynamic.annotation.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    /**
     * 查询指定餐厅的所有菜品
     */
    @Select("SELECT * FROM dishes WHERE restaurantId = #{restaurantId}")
    List<Dish> findByRestaurantId(@Param("restaurantId") Long restaurantId);
}