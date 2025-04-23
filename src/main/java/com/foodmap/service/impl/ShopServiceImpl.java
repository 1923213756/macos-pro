package com.foodmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodmap.entity.dto.ShopInfoUpdateDTO;
import com.foodmap.mapper.ShopMapper;
import com.foodmap.entity.pojo.Shop;
import com.foodmap.entity.dto.ShopAuthInfo;
import com.foodmap.exception.*;
import com.foodmap.security.service.JwtUserDetailsService;
import com.foodmap.service.ShopService;
import com.foodmap.util.RedisCacheUtil;
import com.foodmap.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper,Shop> implements ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopServiceImpl.class);

    private static final org.junit.platform.commons.logging.Logger logger = org.junit.platform.commons.logging.LoggerFactory.getLogger(JwtUserDetailsService.class);


    private final ShopMapper shopMapper;
    private final RedisCacheUtil cacheUtil;

    @Autowired
    public ShopServiceImpl(ShopMapper shopMapper, RedisCacheUtil cacheUtil) {
        this.shopMapper = shopMapper;
        this.cacheUtil = cacheUtil;
    }

    // 商铺注册 (不变，但添加缓存清理)
    @Override
    public void register(Shop shop) {
        // 密码加密、储存
        String encryptedPwd = SecurityUtil.encryptPassword(shop.getPassword());
        shop.setPassword(encryptedPwd);

        // 设置默认营业时间（可选）
        if (StringUtils.isEmpty(shop.getBusinessHours())) {
            shop.setBusinessHours("09:00-21:00");
        }

        // 插入数据库
        if (shopMapper.insertShop(shop) != 1) {
            throw new BadRequestException("商铺注册失败");
        }

        // 清除商铺列表缓存
        cacheUtil.deleteByPattern("shops:list:*");
    }

    @Override
    public Shop login(String shopName, String rawPassword) {
        // 执行数据库查询
        Shop shop = shopMapper.selectByShopName(shopName);

        // 验证密码 - 使用Lambda表达式
        boolean passwordMatch = SecurityUtil.checkPassword(rawPassword, shop.getPassword());

        if (!passwordMatch) {
            throw new UnauthorizedException("密码错误");
        }

        return shop;
    }

    // 商铺列表查询 (添加缓存)
    @Override
    public List<Shop> queryShopList(String category, String district, String sortField)  {
        // 默认按评分降序排序
        if (sortField == null || sortField.isEmpty()) {
            sortField = "composite_score";
        }

        // 构建缓存key
        String cacheKey = "shops:list:" + (category == null ? "all" : category) + ":"
                + (district == null ? "all" : district) + ":" + sortField;

        // 尝试从缓存获取
        Object cachedData = cacheUtil.get(cacheKey);
        if (cachedData != null) {
            return (List<Shop>) cachedData;
        }

        // 缓存未命中，从数据库获取
        List<Shop> shops = shopMapper.selectShopList(category, district, sortField);

        // 记录数据完整性检查
        if (shops != null && !shops.isEmpty()) {
            Shop firstShop = shops.getFirst();
            log.info("数据库获取商铺列表，第一个商铺: id={}, name={}, address={}",
                    firstShop.getShopId(),
                    firstShop.getShopName(),
                    firstShop.getAddress());
        }

        // 存入缓存
        if (shops != null && !shops.isEmpty()) {
            cacheUtil.set(cacheKey, shops);
        }

        return shops;
    }

    // 获取商铺详情 (添加缓存)
    @Override
    public Shop getShopById(Long shopId) {
        if (shopId == null) {
            throw new BadRequestException("商铺ID不能为空");
        }

        // 构建缓存key
        String cacheKey = "shops:id:" + shopId;

        // 尝试从缓存获取
        Object cachedData = cacheUtil.get(cacheKey);
        if (cachedData != null) {
            return (Shop) cachedData;
        }

        // 缓存未命中，从数据库获取
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new NotFoundException("商铺不存在");
        }

        // 记录日志
        log.info("数据库获取商铺详情: id={}, name={}, address={}",
                shop.getShopId(), shop.getShopName(), shop.getAddress());

        // 存入缓存
        cacheUtil.set(cacheKey, shop);

        return shop;
    }

    // 更新商铺状态 (添加缓存清理)
    @Override
    public void updateShopStatus(Long shopId, Integer status) {
        if (status != 0 && status != 1) {
            throw new BadRequestException("无效的商铺状态");
        }

        // 先检查商铺是否存在
        if (shopMapper.selectById(shopId) == null) {
            throw new NotFoundException("商铺不存在");
        }

        Shop shop = new Shop();
        shop.setShopId(shopId);
        shop.setStatus(status);

        if (shopMapper.updateShopStatus(shop) != 1) {
            throw new BadRequestException("更新商铺状态失败");
        }

        // 清除相关缓存
        cacheUtil.delete("shops:id:" + shopId);
        cacheUtil.deleteByPattern("shops:list:*");
    }

    // 更新商铺信息 (添加缓存清理)
/**
 * 更新商铺信息（不包含密码）
 * @return 是否更新成功
 */
@Override
public boolean updateShopInfo(ShopInfoUpdateDTO dto) {
    // 1. 创建更新条件包装器
    UpdateWrapper<Shop> updateWrapper = new UpdateWrapper<>();

    // 2. 根据DTO中的shopId设置更新条件
    updateWrapper.eq("shopId", dto.getShopId());

    // 3. 只更新非null的特定字段
    // 注意: 保持与原代码相同的数据库字段名格式
    updateWrapper
            .set(dto.getShopName() != null, "shopName", dto.getShopName())
            .set(dto.getAddress() != null, "address", dto.getAddress())
            .set(dto.getContactTel() != null, "contactTel", dto.getContactTel())
            .set(dto.getBusinessHours() != null, "businessHours", dto.getBusinessHours())
            .set(dto.getCategory() != null, "category", dto.getCategory())
            .set(dto.getDistrict() != null, "district", dto.getDistrict())
            .set(dto.getDescription() != null, "description", dto.getDescription())
            .set(dto.getStatus() != null, "status", dto.getStatus());

    // 4. 执行更新操作
    boolean updated = this.update(updateWrapper);

    // 5. 如果需要清除缓存，可以添加缓存清理代码
    if (updated && cacheUtil != null) {
        cacheUtil.delete("shops:id:" + dto.getShopId());
        cacheUtil.deleteByPattern("shops:list:*");
    }

    return updated;
}

    // 根据名称获取商铺 (添加缓存)
    @Override
    public Shop getShopByName(String shopName) {
        // 参数校验
        if (!StringUtils.hasText(shopName)) {
            throw new BadRequestException("商铺名称不能为空");
        }

        // 构建缓存key
        String cacheKey = "shops:name:" + shopName;

        // 尝试从缓存获取
        Object cachedData = cacheUtil.get(cacheKey);
        if (cachedData != null) {
            return (Shop) cachedData;
        }

        // 从数据库查询商铺信息
        Shop shop = shopMapper.selectByShopName(shopName);

        // 商铺不存在时的处理
        if (shop == null) {
            throw new NotFoundException("商铺不存在");
        }

        // 记录日志
        log.info("数据库获取商铺详情(按名称): id={}, name={}, address={}",
                shop.getShopId(), shop.getShopName(), shop.getAddress());

        // 存入缓存
        cacheUtil.set(cacheKey, shop);

        return shop;
    }

    // 删除商铺 (添加缓存清理)
    @Override
    public void deleteShop(Long shopId, String shopName, String password) {
        // 1. 查询店铺基本信息（仅用于验证）
        ShopAuthInfo info = shopMapper.selectShopAuthInfo(shopId);

        if (info == null) {
            throw new NotFoundException("商铺不存在，无法删除");
        }

        // 2. 验证店铺名称
        if (!info.getShopName().equals(shopName)) {
            throw new ForbiddenException("店铺名称不匹配，验证失败");
        }

        // 3. 验证密码
        if (!SecurityUtil.checkPassword(password, info.getPassword())) {
            throw new ForbiddenException("密码验证失败，无法删除商铺");
        }

        // 4. 执行删除操作
        int result = shopMapper.deleteShopById(shopId);

        if (result != 1) {
            throw new BadRequestException("删除商铺失败");
        }

        // 清除相关缓存
        cacheUtil.delete("shops:id:" + shopId);
        cacheUtil.delete("shops:name:" + shopName);
        cacheUtil.deleteByPattern("shops:list:*");
    }

    //更新商铺密码
    @Override
    public void updateShopPassword(Long shopId, String oldPassword, String newPassword) {
        // 参数验证
        if (shopId == null || shopId <= 0) {
            throw new BadRequestException("无效的商铺ID");
        }

        if (!StringUtils.hasText(oldPassword)) {
            throw new BadRequestException("当前密码不能为空");
        }

        if (!StringUtils.hasText(newPassword)) {
            throw new BadRequestException("新密码不能为空");
        }

        if (oldPassword.equals(newPassword)) {
            throw new BadRequestException("新密码不能与当前密码相同");
        }

        // 获取商铺信息
        Shop shop = getById(shopId);
        if (shop == null) {
            throw new NotFoundException("商铺不存在");
        }

        // 验证旧密码
        if (!SecurityUtil.checkPassword(oldPassword, shop.getPassword())) {
            throw new UnauthorizedException("当前密码不正确");
        }

        // 加密新密码
        String encryptedNewPassword = SecurityUtil.encryptPassword(newPassword);

        // 使用LambdaUpdateWrapper更新密码
        LambdaUpdateWrapper<Shop> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Shop::getShopId, shopId)
                .set(Shop::getPassword, encryptedNewPassword)
                .set(Shop::getUpdateTime, LocalDateTime.now());

        boolean updated = update(updateWrapper);
        if (!updated) {
            throw new BadRequestException("密码更新失败");
        }

        // 清除相关缓存
        cacheUtil.delete("shops:id:" + shopId);
        cacheUtil.delete("shops:name:" + shop.getShopName());

        log.info("商铺 {} ({}) 密码修改成功", shopId, shop.getShopName());
    }
}