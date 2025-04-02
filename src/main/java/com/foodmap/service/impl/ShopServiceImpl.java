package com.foodmap.service.impl;

import com.foodmap.dao.ShopMapper;
import com.foodmap.entity.Shop;
import com.foodmap.entity.ShopAuthInfo;
import com.foodmap.exception.*;
import com.foodmap.service.ShopService;
import com.foodmap.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ShopServiceImpl implements ShopService {

    private final ShopMapper shopMapper;

    @Autowired
    public ShopServiceImpl(ShopMapper shopMapper) {
        this.shopMapper = shopMapper;
    }

    // 商铺注册
    @Override
    public void register(Shop shop) {
        // 格式检查
        validateRegistration(shop);

        // 密码加密、储存
        String encryptedPwd = SecurityUtil.encryptPassword(shop.getShopPassword());
        shop.setShopPassword(encryptedPwd);

        // 设置默认营业时间（可选）
        if (StringUtils.isEmpty(shop.getBusinessHours())) {
            shop.setBusinessHours("09:00-21:00");
        }

        // 插入数据库
        if (shopMapper.insertShop(shop) != 1) {
            throw new BadRequestException("商铺注册失败");
        }
    }

    // 登录逻辑
    @Override
    public Shop login(String shopName, String rawPassword) {
        Shop shop = shopMapper.selectByShopName(shopName);
        if (shop == null) {
            throw new UnauthorizedException("商铺名不存在");
        }

        if (!SecurityUtil.checkPassword(rawPassword, shop.getShopPassword())) {
            throw new UnauthorizedException("密码错误");
        }
        return shop;
    }

    //注册参数校验（更新）
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

    // 商铺列表查询
    @Override
    public List<Shop> queryShopList(String category, String district, String sortField) {
        // 默认按评分降序排序
        if (sortField == null || sortField.isEmpty()) {
            sortField = "composite_score";
        }

        return shopMapper.selectShopList(category, district, sortField);
    }

    // 获取商铺详情
    @Override
    public Shop getShopById(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new NotFoundException("商铺不存在");
        }
        return shop;
    }

    // 更新商铺状态
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
    }

    // 更新商铺信息
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
    }

    @Override
    public Shop getShopByName(String shopName) {
        // 参数校验
        if (!StringUtils.hasText(shopName)) {
            throw new BadRequestException("商铺名称不能为空");
        }

        // 从数据库查询商铺信息
        Shop shop = shopMapper.selectByShopName(shopName);

        // 商铺不存在时的处理
        if (shop == null) {
            throw new NotFoundException("商铺不存在");
        }

        return shop;
    }


    //删除商铺
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
    }
}