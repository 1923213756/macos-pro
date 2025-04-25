package com.foodmap.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodmap.entity.pojo.Dish;
import com.foodmap.mapper.DishMapper;
import com.foodmap.service.DishService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Override
    public List<Dish> getDishesByRestaurantId(Long restaurantId) {
        return lambdaQuery().eq(Dish::getRestaurantId, restaurantId).list();
    }

    @Override
    public List<Dish> getSpecialDishesByRestaurantId(Long restaurantId) {
        return lambdaQuery()
                .eq(Dish::getRestaurantId, restaurantId)
                .eq(Dish::getIsSpecial, 1)
                .list();
    }

    @Override
    public boolean updateDishAvailability(Long dishId, Integer isAvailable) {
        return lambdaUpdate()
                .eq(Dish::getId, dishId)
                .set(Dish::getIsAvailable, isAvailable)
                .update();
    }

    @Override
    public boolean deleteDishes(List<Long> dishIds) {
        return removeByIds(dishIds);
    }
}