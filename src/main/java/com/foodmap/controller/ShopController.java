package com.foodmap.controller;

import com.foodmap.entity.Shop;
import com.foodmap.service.ShopService;
import com.foodmap.vo.LoginRequest;
import com.foodmap.vo.ResponseResult;
import com.foodmap.vo.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@CrossOrigin(origins = "*")  // 用于开发阶段，生产环境应限制来源
public class ShopController {

    private final ShopService shopService;

    @Autowired
    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    /**
     * 商铺注册
     */
    @PostMapping("/register")
    public ResponseResult register(@RequestBody Shop shop) {
        shopService.register(shop);
        return ResponseResult.success("注册成功");
    }

    /**
     * 商铺登录
     */
    @PostMapping("/login")
    public ResponseResult login(@RequestBody LoginRequest request) {
        Shop shop = shopService.login(request.getShopName(), request.getPassword());
        return ResponseResult.success("登录成功", shop);
    }

    /**
     * 获取商铺列表
     */
    @GetMapping
    public ResponseResult getShopList(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String sortField) {
        List<Shop> shops = shopService.queryShopList(category, district, sortField);
        return ResponseResult.success(shops);
    }

    /**
     * 获取商铺详情
     */
    @GetMapping("/{shopId}")
    public ResponseResult getShopById(@PathVariable Long shopId) {
        Shop shop = shopService.getShopById(shopId);
        return ResponseResult.success(shop);
    }

    /**
     * 更新商铺信息
     */
    @PutMapping("/{shopId}")
    public ResponseResult updateShop(@PathVariable Long shopId, @RequestBody Shop shop) {
        shop.setShopId(shopId);  // 确保ID正确
        shopService.updateShopInfo(shop);
        return ResponseResult.success("更新成功");
    }

    /**
     * 更新商铺状态（营业/休息）
     */
    @PutMapping("/{shopId}/status")
    public ResponseResult updateStatus(
            @PathVariable Long shopId,
            @RequestParam Integer status) {
        shopService.updateShopStatus(shopId, status);
        return ResponseResult.success("状态更新成功");
    }

    /**
     * 删除商铺（需要验证）
     */
    @DeleteMapping("/{shopId}")
    public ResponseResult deleteShop(
            @PathVariable Long shopId,
            @RequestParam String shopName,
            @RequestParam String password) {
        shopService.deleteShop(shopId, shopName, password);
        return ResponseResult.success("删除成功");
    }
}