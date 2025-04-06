package com.foodmap.service.impl;

import com.foodmap.dao.ShopMapper;
import com.foodmap.entity.Shop;
import com.foodmap.entity.ShopAuthInfo;
import com.foodmap.exception.*;
import com.foodmap.service.ShopService;
import com.foodmap.util.RedisCacheUtil;
import com.foodmap.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ShopServiceImpl implements ShopService {
    private static final Logger log = LoggerFactory.getLogger(ShopServiceImpl.class);

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
        // 格式检查
        validateRegistration(shop);

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

    // 登录逻辑 (不变)
    @Override
    public Shop login(String shopName, String rawPassword) {
        Shop shop = shopMapper.selectByShopName(shopName);
        if (shop == null) {
            throw new UnauthorizedException("商铺名不存在");
        }

        if (!SecurityUtil.checkPassword(rawPassword, shop.getPassword())) {
            throw new UnauthorizedException("密码错误");
        }
        return shop;
    }

    // 注册参数校验 (不变)
    @Override
    public void validateRegistration(Shop shop) {
        // 基础校验
        if (!StringUtils.hasText(shop.getShopName())) {
            throw new BadRequestException("商铺名不能为空");
        }
        if (!StringUtils.hasText(shop.getShopName())) {
            throw new BadRequestException("密码不能为空");
        }

        // 新增字段校验
        if (!StringUtils.hasText(shop.getAddress())) {
            throw new BadRequestException("商铺地址不能为空");
        }
        if (!StringUtils.hasText(shop.getContactTel())) {
            throw new BadRequestException("联系电话不能为空");
        } else if (!shop.getContactTel().matches("^1[3-9]\\d{9}$")) {
            throw new BadRequestException("联系电话格式错误");
        }

        // 分类和地区校验
        if (!StringUtils.hasText(shop.getCategory())) {
            throw new BadRequestException("商铺分类不能为空");
        }
        if (!StringUtils.hasText(shop.getDistrict())) {
            throw new BadRequestException("商铺所在地区不能为空");
        }

        // 分类限制校验
        String category = shop.getCategory();
        if (!category.equals("甜品") && !category.equals("火锅") && !category.equals("料理")
                && !category.equals("中餐") && !category.equals("西餐") && !category.equals("快餐")) {
            throw new BadRequestException("商铺分类不在允许范围内");
        }

        // 唯一性校验
        if (shopMapper.countByShopName(shop.getShopName()) > 0) {
            throw new ConflictException("商铺名已存在");
        }
        if (shopMapper.countByContactTel(shop.getContactTel()) > 0) {
            throw new ConflictException("联系电话已注册");
        }
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
    @Override
    public void updateShopInfo(Shop shop) {
        // 确保不会更新密码和敏感字段
        if (shop.getShopName() != null) {
            throw new ForbiddenException("不允许通过此方法修改密码");
        }

        // 检查商铺是否存在
        if (shopMapper.selectById(shop.getShopId()) == null) {
            throw new NotFoundException("要更新的商铺不存在");
        }

        // 如果更新分类，需要验证
        if (shop.getCategory() != null) {
            String category = shop.getCategory();
            if (!category.equals("甜品") && !category.equals("火锅") && !category.equals("料理")
                    && !category.equals("中餐") && !category.equals("西餐") && !category.equals("快餐")) {
                throw new BadRequestException("商铺分类不在允许范围内");
            }
        }

        if (shopMapper.updateShopInfo(shop) != 1) {
            throw new BadRequestException("更新商铺信息失败");
        }

        // 清除相关缓存
        cacheUtil.delete("shops:id:" + shop.getShopId());
        cacheUtil.deleteByPattern("shops:list:*");
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
}