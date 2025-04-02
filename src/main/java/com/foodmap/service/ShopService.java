package com.foodmap.service;

import com.foodmap.entity.Shop;
import com.foodmap.entity.ShopAuthInfo;

import java.util.List;

public interface ShopService {
    Shop login(String shopName, String rawPassword);
    void validateRegistration(Shop shop);
    void register(Shop shop);

    //新增内容（未测试）
    Shop getShopByName(String shopName);
    List<Shop> queryShopList(String category, String district, String sortField);
    Shop getShopById(Long shopId);
    void updateShopStatus(Long shopId, Integer status);
    void updateShopInfo(Shop shop);
    void deleteShop(Long shopId, String shopName, String password);
}
