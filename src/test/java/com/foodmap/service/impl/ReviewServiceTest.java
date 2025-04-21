package com.foodmap.service.impl;

import com.foodmap.entity.dto.ReviewCreateDTO;
import com.foodmap.entity.dto.ReviewDTO;
import com.foodmap.entity.pojo.Review;
import com.foodmap.entity.pojo.Shop;
import com.foodmap.entity.pojo.User;
import com.foodmap.mapper.ReviewMapper;
import com.foodmap.mapper.ShopMapper;
import com.foodmap.mapper.UserMapper;
import com.foodmap.service.ReviewService;
import com.foodmap.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ReviewServiceTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewMapper reviewMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    private Long testShopId;
    private Long testUserId;
    private Long testReviewId;

    @BeforeEach
    void setUp() {
        // 创建测试商铺
        Shop shop = new Shop();
        shop.setShopName("测试餐厅");
        shop.setAddress("测试地址");
        shop.setContactTel("13800138000");
        shop.setCategory("中餐");
        shop.setDistrict("天河区");
        shop.setStatus(1);
        shop.setPassword("Test1234");
        shop.setCompositeScore(4.5f);
        shop.setReviewCount(0L);
        shopMapper.insert(shop);
        testShopId = shop.getShopId();

        // 创建测试用户 - 添加所有必要字段
        User user = new User();
        user.setUserName("testuser");
        user.setPassword("$2a$10$XQCfUdyUZhdsbQLe5qWdPxUEaSRYY0Pyi4XlTgJ/fG.1vC"); // 加密的"testpassword"
        user.setPhone("13900139000"); // 添加必填的手机号
        user.setCreateTime(LocalDateTime.now()); // 设置创建时间
        user.setUpdateTime(LocalDateTime.now()); // 设置更新时间
        user.setStatus(1); // 设置状态，虽然有默认值，但为了清晰也设置

        userMapper.insert(user);
        testUserId = user.getUserId();

        // 设置当前认证上下文
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_USER"));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("testuser", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 创建一条测试评论
        Review review = new Review();
        review.setContent("初始评论内容");
        review.setRating(4);
        review.setUserId(testUserId);
        review.setRestaurantId(testShopId);
        review.setStatus(Review.STATUS_ACTIVE);
        // 手动设置时间字段
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
        testReviewId = review.getId();
    }

    @AfterEach
    void tearDown() {
        // 清理认证上下文
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateReview() {
        // 准备数据
        ReviewCreateDTO createDTO = new ReviewCreateDTO();
        createDTO.setContent("食物非常美味，服务很好");
        createDTO.setRating(5);
        createDTO.setRestaurantId(testShopId);

        // 执行创建评论
        ReviewDTO result = reviewService.createReview(createDTO);

        // 验证结果
        assertNotNull(result);
        assertEquals("食物非常美味，服务很好", result.getContent());
        assertEquals(5, result.getRating());
        assertEquals(testUserId, result.getUserId());
        assertEquals(testShopId, result.getRestaurantId());

        // 验证商铺评分是否更新
        Shop updatedShop = shopMapper.selectById(testShopId);
        assertNotNull(updatedShop);
        assertEquals(2L, updatedShop.getReviewCount());  // 初始评论 + 新评论
    }

    // 其他测试方法...
}