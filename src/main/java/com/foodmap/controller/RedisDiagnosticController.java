package com.foodmap.controller;

import com.foodmap.common.response.ResponseResult;
import com.foodmap.mapper.ShopMapper;
import com.foodmap.entity.pojo.Shop;
import com.foodmap.util.RedisCacheUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class RedisDiagnosticController {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private RedisCacheUtil cacheUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Logger log = LoggerFactory.getLogger(RedisDiagnosticController.class);

    @GetMapping("/check-shop-data")
    public ResponseResult<Map<String, Object>> checkShopData() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 从数据库获取一条商铺数据
            List<Shop> shops = shopMapper.selectShopList(null, null, "composite_score");
            if (shops == null || shops.isEmpty()) {
                return ResponseResult.error(404, "数据库中没有商铺数据");
            }

            Shop shop = shops.get(0);
            log.info("数据库查询结果: id={}, name={}, address={}",
                    shop.getShopId(), shop.getShopName(), shop.getAddress());

            // 使用HashMap代替Map.of() - 避免空值问题
            Map<String, Object> dbData = new HashMap<>();
            dbData.put("id", shop.getShopId());
            dbData.put("name", shop.getShopName());
            dbData.put("address", shop.getAddress());

            // 记录一下哪些字段为空，方便调试
            if (shop.getShopId() == null) dbData.put("id_is_null", true);
            if (shop.getShopName() == null) dbData.put("name_is_null", true);
            if (shop.getAddress() == null) dbData.put("address_is_null", true);

            result.put("database", dbData);

            // 将该商铺缓存到Redis
            String cacheKey = "shops:debug:" + shop.getShopId();
            cacheUtil.set(cacheKey, shop);

            // 从Redis读取该商铺
            Object cachedShop = cacheUtil.get(cacheKey);
            if (cachedShop != null) {
                Shop redisShop = (Shop) cachedShop;

                // 同样使用HashMap而非Map.of()
                Map<String, Object> redisData = new HashMap<>();
                redisData.put("id", redisShop.getShopId());
                redisData.put("name", redisShop.getShopName());
                redisData.put("address", redisShop.getAddress());

                // 记录空值情况
                if (redisShop.getShopId() == null) redisData.put("id_is_null", true);
                if (redisShop.getShopName() == null) redisData.put("name_is_null", true);
                if (redisShop.getAddress() == null) redisData.put("address_is_null", true);

                result.put("redis", redisData);
            } else {
                result.put("redis", "缓存数据为空");
            }

            return ResponseResult.success(result);
        } catch (Exception e) {
            log.error("检查商铺数据出错", e);
            return ResponseResult.error(500, "检查商铺数据出错: " + e.getMessage());
        }
    }

    @GetMapping("/clear-cache")
    public ResponseResult<String> clearCache() {
        try {
            // 清除所有商铺相关缓存
            cacheUtil.deleteByPattern("shops:*");
            return ResponseResult.success("所有商铺缓存已清除");
        } catch (Exception e) {
            log.error("清除缓存出错", e);
            return ResponseResult.error(500, "清除缓存出错: " + e.getMessage());
        }
    }
}