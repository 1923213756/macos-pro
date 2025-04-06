package com.foodmap.controller;

import com.foodmap.dao.ShopMapper;
import com.foodmap.entity.Shop;
import com.foodmap.security.jwt.JwtTokenProvider;
import com.foodmap.service.ShopService;
import com.foodmap.common.response.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shops")
@CrossOrigin(origins = "*")  // 用于开发阶段，生产环境应限制来源
@Tag(name = "商铺管理", description = "商铺注册、登录、查询、更新与删除操作")
public class ShopController {

    private final ShopService shopService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Autowired
    public ShopController(ShopService shopService, AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider, ShopMapper shopMapper) {
        this.shopService = shopService;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    /**
     * 商铺注册
     */
    @Operation(summary = "商铺注册", description = "注册新商铺账户，提供商铺基本信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "400", description = "注册信息有误或商铺名已存在",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @PostMapping("/register")
    public ResponseResult<Shop> register(
            @Parameter(description = "商铺注册信息", required = true)
            @RequestBody Shop shop) {
        shopService.register(shop);
        return ResponseResult.success("注册成功",shop);
    }

    /**
     * 商铺登录
     */
    @Operation(summary = "商铺登录", description = "使用商铺名和密码进行登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功，返回商铺信息",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "401", description = "商铺名或密码错误",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @PostMapping("/login")
    public ResponseResult<Map<String, Object>> login(
            @Parameter(description = "登录请求信息，包含商铺名和密码", required = true)
            @RequestBody Shop request,
            HttpServletResponse response) {

        // 保留原有的登录逻辑
        Shop shop = shopService.login(request.getShopName(), request.getPassword());

        if (shop != null) {
            try {
                // 创建认证对象
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getShopName(),
                                request.getPassword()
                        )
                );

                // 设置认证信息到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 生成JWT令牌
                String jwt = tokenProvider.generateToken(authentication);

                // 出于安全考虑，清除密码
                shop.setPassword(null);

                // 构建响应数据，包含商铺信息和token
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("shop", shop);
                responseData.put("token", jwt);

                return ResponseResult.success("登录成功", responseData);
            } catch (Exception e) {
                // 记录异常，这里可以添加日志
                return ResponseResult.error(500, "认证处理异常: " + e.getMessage());
            }
        } else {
            return ResponseResult.error(401, "商铺名或密码错误");
        }
    }

    /**
     * 获取商铺列表
     */
    @Operation(summary = "获取商铺列表", description = "获取商铺列表，支持按类别、区域筛选和排序")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取商铺列表",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('SHOP') or hasRole('USER')")
    public ResponseResult<List<Shop>> getShopList(
            @Parameter(description = "商铺类别，如'中餐'、'西餐'等")
            @RequestParam(required = false) String category,

            @Parameter(description = "商铺所在区域，如'海珠区'、'白云区'等")
            @RequestParam(required = false) String district,

            @Parameter(description = "排序字段，如'compositeScore'(评分)、'createTime'(创建时间)等")
            @RequestParam(required = false) String sortField) {
        List<Shop> shops = shopService.queryShopList(category, district, sortField);
        return ResponseResult.success(shops);
    }

    /**
     * 获取商铺详情
     */
    @Operation(summary = "获取商铺详情", description = "根据商铺ID获取商铺详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取商铺详情",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "404", description = "指定ID的商铺不存在",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @GetMapping("/{shopId}")
    @PreAuthorize("hasRole('SHOP') or hasRole('USER')")
    public ResponseResult<Shop> getShopById(
            @Parameter(description = "商铺ID", required = true)
            @PathVariable Long shopId) {
        Shop shop = shopService.getShopById(shopId);
        return ResponseResult.success(shop);
    }

    /**
     * 更新商铺信息
     */
    @Operation(summary = "更新商铺信息", description = "更新指定商铺的信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "404", description = "指定ID的商铺不存在",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "403", description = "无权更新此商铺信息",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @PutMapping("/{shopId}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseResult<Void> updateShop(
            @Parameter(description = "商铺ID", required = true)
            @PathVariable Long shopId,

            @Parameter(description = "更新的商铺信息", required = true)
            @RequestBody Shop shop) {
        shop.setShopId(shopId);  // 确保ID正确
        shopService.updateShopInfo(shop);
        return ResponseResult.success("更新成功");
    }

    /**
     * 更新商铺状态（营业/休息）
     */
    @Operation(summary = "更新商铺状态", description = "更新商铺的营业状态，如营业中(1)或休息中(0)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "状态更新成功",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "404", description = "指定ID的商铺不存在",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "403", description = "无权更新此商铺状态",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @PutMapping("/{shopId}/status")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseResult<Void> updateStatus(
            @Parameter(description = "商铺ID", required = true)
            @PathVariable Long shopId,

            @Parameter(description = "商铺状态：0-休息中，1-营业中", required = true, example = "1")
            @RequestParam Integer status) {
        shopService.updateShopStatus(shopId, status);
        return ResponseResult.success("状态更新成功");
    }

    /**
     * 删除商铺（需要验证）
     */
    @Operation(summary = "删除商铺", description = "删除指定的商铺，需要提供商铺名和密码进行验证")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "404", description = "指定ID的商铺不存在",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "401", description = "验证失败，商铺名或密码错误",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @DeleteMapping("/{shopId}")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseResult<Void> deleteShop(
            @Parameter(description = "商铺ID", required = true)
            @PathVariable Long shopId,

            @Parameter(description = "商铺名称", required = true)
            @RequestParam String shopName,

            @Parameter(description = "商铺密码", required = true)
            @RequestParam String password) {
        shopService.deleteShop(shopId, shopName, password);
        return ResponseResult.success("删除成功");
    }
}