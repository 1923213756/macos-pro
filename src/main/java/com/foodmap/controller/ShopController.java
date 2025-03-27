package com.foodmap.controller;

import com.foodmap.entity.Shop;
import com.foodmap.service.ShopService;
import com.foodmap.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shops")
public class ShopController {

    @Autowired
    private ShopService shopService;

    // 商铺列表查询
    @GetMapping
    public Result<List<Shop>> queryShopList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String sortField) {
        List<Shop> shopList = shopService.queryShopList(category, district, sortField);
        return Result.success(shopList);
    }

    // 商铺详情查询
    @GetMapping("/{id}")
    public Result<Shop> getShopDetail(@PathVariable("id") Long shopId) {
        Shop shop = shopService.getShopById(shopId);
        return Result.success(shop);
    }

    // 更新商铺状态
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable("id") Long shopId,
            @RequestParam Integer status) {
        shopService.updateShopStatus(shopId, status);
        return Result.success();
    }

    // 更新商铺信息
    @PutMapping("/{id}")
    public Result<Void> updateShopInfo(
            @PathVariable("id") Long shopId,
            @RequestBody Shop shop) {
        shop.setShopId(shopId);
        shopService.updateShopInfo(shop);
        return Result.success();
    }

    @PostMapping
    public Result<Shop> register(@RequestBody Shop shop) {
        void insertedShop = shopService.register(shop);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteShop (@PathVariable("id") Long shopId){
        shopService.deleteShop(shopId);
        return Result.success();
    }

    //分页功能，防止数据过载（未完善）
//    @GetMapping
//    public Result<Page<Shop>> queryShopList(
//            @RequestParam(required = false) String category,
//            @RequestParam(required = false) String district,
//            @RequestParam(required = false) String sortField,
//            @RequestParam(defaultValue = "1") Integer page,
//            @RequestParam(defaultValue = "10") Integer size) {
//        Page<Shop> shopPage = shopService.queryShopList(category, district, sortField, page, size);
//        return Result.success(shopPage);
//    }

}