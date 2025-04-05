package com.foodmap.controller;

import com.foodmap.common.response.ResponseResult;
import com.foodmap.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseResult<Map<String, Object>> authenticateUser(@RequestBody LoginRequest loginRequest,
                                                                HttpServletResponse response) {
        // 验证用户名和密码
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 设置认证信息到安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 生成JWT令牌
        String jwt = tokenProvider.generateToken(authentication);

        // 返回JWT和用户信息
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", jwt);
        responseData.put("tokenType", "Bearer");

        return ResponseResult.success("登录成功", responseData);
    }

    @PostMapping("/logout")
    public ResponseResult<Object> logoutUser(HttpServletResponse response) {
        // 清除安全上下文
        SecurityContextHolder.clearContext();

        return ResponseResult.success("注销成功");
    }

    // 登录请求对象
    @Setter
    @Getter
    public static class LoginRequest {
        private String username;
        private String password;
        private boolean rememberMe;
    }
}