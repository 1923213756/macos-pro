package com.foodmap.controller;

import com.foodmap.entity.Shop;
import com.foodmap.service.ShopService;
import com.foodmap.vo.LoginRequest;
import com.foodmap.vo.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shops")
@CrossOrigin(origins = "*")  // 用于开发阶段，生产环境应限制来源
@Tag(name = "商铺管理", description = "商铺注册、登录、查询、更新与删除操作")
public class ShopController {

    private final ShopService shopService;

    @Autowired
    public ShopController(ShopService shopService) {
        this.shopService = shopService;
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
    public ResponseResult<Void> register(
            @Parameter(description = "商铺注册信息", required = true)
            @RequestBody Shop shop) {
        shopService.register(shop);
        return ResponseResult.success("注册成功");
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
    public ResponseResult<Shop> login(
            @Parameter(description = "登录请求信息，包含商铺名和密码", required = true)
            @RequestBody LoginRequest request) {
        Shop shop = shopService.login(request.getShopName(), request.getPassword());
        return ResponseResult.success("登录成功", shop);
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
    public ResponseResult<List<Shop>> getShopList(
            @Parameter(description = "商铺类别，如'中餐'、'西餐'等")
            @RequestParam(required = false) String category,

            @Parameter(description = "商铺所在区域，如'朝阳区'、'海淀区'等")
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