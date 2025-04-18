package com.foodmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodmap.entity.dto.ShopInfoUpdateDTO;
import com.foodmap.entity.pojo.Shop;

import java.util.List;

public interface ShopService{
    Shop login(String shopName, String rawPassword);
    void register(Shop shop);

    //新增内容（未测试）
    Shop getShopByName(String shopName);
    List<Shop> queryShopList(String category, String district, String sortField);
    Shop getShopById(Long shopId);
    void updateShopStatus(Long shopId, Integer status);
    boolean updateShopInfo(ShopInfoUpdateDTO dto);
    void deleteShop(Long shopId, String shopName, String password);

    void updateShopPassword(Long shopId, String oldPassword, String newPassword);
}
