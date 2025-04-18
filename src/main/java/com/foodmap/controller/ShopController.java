package com.foodmap.controller;

import com.foodmap.entity.dto.ShopInfoUpdateDTO;
import com.foodmap.mapper.ShopMapper;
import com.foodmap.entity.pojo.Shop;
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
import jakarta.validation.Valid;
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
            @Valid @RequestBody Shop shop) {
        shopService.register(shop);
        return ResponseResult.success("注册成功", shop);
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
            @Valid @RequestBody Shop request) {

        // 验证登录请求数据
        if (request.getShopName() == null || request.getShopName().trim().isEmpty()) {
            return ResponseResult.error(400, "商铺名称不能为空");
        }

        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseResult.error(400, "密码不能为空");
        }

        // 保留原有的登录逻辑
        Shop shop = shopService.login(request.getShopName(), request.getPassword());
        if (shop != null) {
            try {
                String shopIdentifier = "SHOP_" + request.getShopName();
                // 创建认证对象
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                shopIdentifier,
                                request.getPassword()
                        )
                );

                // 设置认证信息到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 生成JWT令牌
                String jwt = tokenProvider.generateToken(authentication);

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

        // 验证排序字段
        if (sortField != null && !sortField.isEmpty() &&
                !sortField.equals("compositeScore") && !sortField.equals("createTime")) {
            return ResponseResult.error(400, "不支持的排序字段，支持的字段为：compositeScore, createTime");
        }

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

        // 验证商铺ID
        if (shopId == null || shopId <= 0) {
            return ResponseResult.error(400, "无效的商铺ID");
        }

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
            @Valid @RequestBody ShopInfoUpdateDTO dto) {

        // 验证商铺ID
        if (shopId == null || shopId <= 0) {
            return ResponseResult.error(400, "无效的商铺ID");
        }

        // 确保DTO中的shopId与路径参数一致
        dto.setShopId(shopId);

        // 调用更新方法
        boolean updated = shopService.updateShopInfo(dto);

        // 根据更新结果返回响应
        if (updated) {
            return ResponseResult.success("更新成功");
        } else {
            return ResponseResult.error("未做任何更新");
        }
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

        // 验证商铺ID
        if (shopId == null || shopId <= 0) {
            return ResponseResult.error(400, "无效的商铺ID");
        }

        // 验证状态值
        if (status == null || (status != 0 && status != 1)) {
            return ResponseResult.error(400, "商铺状态值必须为0(休息中)或1(营业中)");
        }

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

        // 验证商铺ID
        if (shopId == null || shopId <= 0) {
            return ResponseResult.error(400, "无效的商铺ID");
        }

        // 验证商铺名称
        if (shopName == null || shopName.trim().isEmpty()) {
            return ResponseResult.error(400, "商铺名称不能为空");
        }

        // 验证密码
        if (password == null || password.trim().isEmpty()) {
            return ResponseResult.error(400, "密码不能为空");
        }

        shopService.deleteShop(shopId, shopName, password);
        return ResponseResult.success("删除成功");
    }

    /**
     * 修改商铺密码
     */
    @Operation(summary = "修改商铺密码", description = "修改指定商铺的登录密码，需要提供当前密码进行验证")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "密码修改成功",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "400", description = "新密码不符合要求",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "401", description = "当前密码验证失败",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "403", description = "无权修改此商铺密码",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    @PutMapping("/{shopId}/password")
    @PreAuthorize("hasRole('SHOP')")
    public ResponseResult<Void> updatePassword(
            @Parameter(description = "商铺ID", required = true)
            @PathVariable Long shopId,

            @Parameter(description = "密码修改信息", required = true)
            @RequestBody Map<String, String> passwordData) {

        // 验证商铺ID
        if (shopId == null || shopId <= 0) {
            return ResponseResult.error(400, "无效的商铺ID");
        }

        // 基本验证 - 确保参数存在
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");

        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return ResponseResult.error(400, "当前密码不能为空");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseResult.error(400, "新密码不能为空");
        }

        // 密码格式验证
        if (newPassword.length() < 8 || newPassword.length() > 20) {
            return ResponseResult.error(400, "新密码长度必须在8-20个字符之间");
        }

        // 密码复杂度验证 (至少包含一个大写字母、一个小写字母和一个数字)
        if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{8,}$")) {
            return ResponseResult.error(400, "新密码至少需包含一个大写字母、一个小写字母和一个数字");
        }

        // 检查新旧密码是否相同
        if (oldPassword.equals(newPassword)) {
            return ResponseResult.error(400, "新密码不能与当前密码相同");
        }

        shopService.updateShopPassword(shopId, oldPassword, newPassword);
        return ResponseResult.success("密码修改成功");
    }
}