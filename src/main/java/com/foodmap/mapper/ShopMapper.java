package com.foodmap.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodmap.entity.pojo.Shop;
import com.foodmap.entity.dto.ShopAuthInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.util.List;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
    //注册商铺
    int insertShop(Shop shop);
    //搜索商铺（未模糊搜索增强）
    Shop selectByShopName(String shopName);

    //根据电话号排序（取消）
    int countByContactTel(String contactTel);

    //根据名字排序
    int countByShopName(String shopName);

    //更新评分排名（待完善）刷新处理
    int updateScore(Shop shop);

    // 新增方法
    List<Shop> selectShopList(@Param("category") String category,
                              @Param("district") String district,
                              @Param("sortField") String sortField);

    //更新商铺状态（营业/非营业）
    int updateShopStatus(Shop shop);

    //更新商铺信息
    int updateShopInfo(Shop shop);

    //根据id筛选
    Shop selectById(Long shopId);

    //根据id删除
    int deleteShopById(Long shopId);

    ShopAuthInfo selectShopAuthInfo(Long shopId);
}