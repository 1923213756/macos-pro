package com.foodmap.controller;

import com.foodmap.entity.User;
import com.foodmap.common.response.ResponseResult;
import com.foodmap.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户注册、登录和信息管理")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，提供用户名、密码和手机号等基本信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "400", description = "注册信息有误、用户名已存在或手机号已注册",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    public ResponseResult<Void> register(@Validated @RequestBody User user) {
        userService.register(user);
        return ResponseResult.success("注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码进行登录验证")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "登录成功，返回用户信息",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "401", description = "用户名或密码错误",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    public ResponseResult<User> login(@RequestBody User request) {
        User user = userService.login(request.getUserName(), request.getUserPassword());
        // 出于安全考虑，可以在返回前清除密码
        user.setUserPassword(null);
        return ResponseResult.success("登录成功", user);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户信息",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "404", description = "用户不存在",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    public ResponseResult<User> getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long id) {
        // 假设有一个根据ID获取用户的方法
        // User user = userService.getUserById(id);
        // return ResponseResult.success(user);

        // 由于UserServiceImpl中没有此方法，这里仅作示例
        return ResponseResult.error(HttpStatus.NOT_IMPLEMENTED.value(), "功能尚未实现");
    }
}