package com.foodmap.controller;

import com.foodmap.common.response.ResponseResult;
import com.foodmap.entity.User;
import com.foodmap.security.jwt.JwtTokenProvider;
import com.foodmap.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户注册、登录和信息管理")
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public UserController(UserService userService, JwtTokenProvider tokenProvider, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "注册新用户，提供用户名、密码和手机号等基本信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "注册成功",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "400", description = "注册信息有误、用户名已存在或手机号已注册",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    public ResponseResult<Void> register(@RequestBody User user) {
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
    public ResponseResult<Map<String, Object>> login(@RequestBody User request, HttpServletResponse response) {

        User user = userService.login(request.getUserName(), request.getPassword());

        if (user != null) {
            try {
                // 创建认证对象
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUserName(),
                                request.getPassword()
                        )
                );

                // 设置认证信息到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 生成JWT令牌
                String jwt = tokenProvider.generateToken(authentication);

                // 出于安全考虑，清除密码
                user.setPassword(null);

                // 构建响应数据，包含用户信息和token
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("user", user);
                responseData.put("token", jwt);

                return ResponseResult.success("登录成功", responseData);
            } catch (Exception e) {
                // 记录异常，这里可以添加日志
                return ResponseResult.error(500, "认证处理异常: " + e.getMessage());
            }
        } else {
            return ResponseResult.error(401, "用户名或密码错误");
        }
    }

    @GetMapping("/{name}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户信息",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class))),
            @ApiResponse(responseCode = "404", description = "用户不存在",
                    content = @Content(schema = @Schema(implementation = ResponseResult.class)))
    })
    public ResponseResult<User> getUserByName(
            @Parameter(description = "用户ID", required = true)
            @PathVariable String name) {
        // 假设有一个根据ID获取用户的方法
        User user = userService.getUserByName(name);
        return ResponseResult.success(user);
    }
}