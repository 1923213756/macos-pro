package com.foodmap.controller;

import com.foodmap.entity.pojo.Dish;
import com.foodmap.service.DishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
public class DishController {

    private final DishService dishService;

    /**
     * 获取餐厅的所有菜品
     *
     * @param restaurantId 餐厅ID
     * @return 菜品列表
     */
    @GetMapping("/{restaurantId}")
    public List<Dish> getDishesByRestaurantId(@PathVariable Long restaurantId) {
        return dishService.getDishesByRestaurantId(restaurantId);
    }

    /**
     * 获取餐厅的特色菜品
     *
     * @param restaurantId 餐厅ID
     * @return 特色菜品列表
     */
    @GetMapping("/{restaurantId}/specials")
    public List<Dish> getSpecialDishesByRestaurantId(@PathVariable Long restaurantId) {
        return dishService.getSpecialDishesByRestaurantId(restaurantId);
    }

    /**
     * 添加新的菜品
     *
     * @param dish 菜品信息
     * @return 添加的菜品
     */
    @PostMapping
    public Dish addDish(@RequestBody Dish dish) {
        dishService.save(dish);
        return dish;
    }

    /**
     * 更新菜品信息
     *
     * @param dish 菜品信息
     * @return 是否更新成功
     */
    @PutMapping
    public boolean updateDish(@RequestBody Dish dish) {
        return dishService.updateById(dish);
    }

    /**
     * 更新菜品的可用状态
     *
     * @param dishId      菜品ID
     * @param isAvailable 是否可用（1：可用，0：不可用）
     * @return 是否更新成功
     */
    @PutMapping("/{dishId}/availability")
    public boolean updateDishAvailability(@PathVariable Long dishId, @RequestParam Integer isAvailable) {
        return dishService.updateDishAvailability(dishId, isAvailable);
    }

    /**
     * 删除菜品
     *
     * @param dishId 菜品ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{dishId}")
    public boolean deleteDish(@PathVariable Long dishId) {
        return dishService.removeById(dishId);
    }

    /**
     * 批量删除菜品
     *
     * @param dishIds 菜品ID列表
     * @return 是否删除成功
     */
    @DeleteMapping
    public boolean deleteDishes(@RequestBody List<Long> dishIds) {
        return dishService.deleteDishes(dishIds);
    }
}