package com.foodmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodmap.entity.pojo.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    /**
     * 获取某个餐厅的所有菜品
     *
     * @param restaurantId 餐厅ID
     * @return 菜品列表
     */
    List<Dish> getDishesByRestaurantId(Long restaurantId);

    /**
     * 获取餐厅的特色菜品
     *
     * @param restaurantId 餐厅ID
     * @return 特色菜品列表
     */
    List<Dish> getSpecialDishesByRestaurantId(Long restaurantId);

    /**
     * 更新菜品的可用状态
     *
     * @param dishId      菜品ID
     * @param isAvailable 是否可用（1：可用，0：不可用）
     * @return 是否更新成功
     */
    boolean updateDishAvailability(Long dishId, Integer isAvailable);

    /**
     * 批量删除菜品
     *
     * @param dishIds 菜品ID列表
     * @return 是否删除成功
     */
    boolean deleteDishes(List<Long> dishIds);
}