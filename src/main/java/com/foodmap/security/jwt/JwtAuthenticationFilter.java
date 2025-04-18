package com.foodmap.security.jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    // 刷新窗口时间，例如5分钟
    private static final long REFRESH_WINDOW_MILLIS = 5 * 60 * 1000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = getTokenFromRequest(request);

            if (token != null) {
                // 尝试验证令牌
                jwtTokenProvider.validateToken(token);

                // 验证通过，设置认证信息
                String username = jwtTokenProvider.getUsernameFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: {}", e.getMessage());

            // 检查是否在刷新窗口期内
            if (isInRefreshWindow(e)) {
                handleTokenRefresh(e, request, response);
                // 继续处理请求
                filterChain.doFilter(request, response);
                return;
            }

            // 如果不在刷新窗口期，重定向到登录页面
            response.sendRedirect("/api/auth/relogin");
            return; // 终止过滤器链，不继续处理请求
        } catch (Exception e) {
            log.error("无法验证JWT令牌: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从HTTP请求中提取JWT令牌
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 判断令牌是否在刷新窗口期内
     */
    private boolean isInRefreshWindow(ExpiredJwtException e) {
        Date expiration = e.getClaims().getExpiration();
        // 计算令牌过期时间和当前时间的差值
        long diffMillis = System.currentTimeMillis() - expiration.getTime();
        // 如果在刷新窗口期内（过期时间不超过设定的刷新窗口时间）
        return diffMillis <= REFRESH_WINDOW_MILLIS;
    }

    /**
     * 处理令牌刷新
     */
    private void handleTokenRefresh(ExpiredJwtException e, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 获取用户名
            String username = e.getClaims().getSubject();
            log.info("自动刷新用户[{}]的JWT令牌", username);

            // 加载用户详情
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 创建Authentication对象
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // 使用Authentication对象生成新令牌
            String newToken = jwtTokenProvider.generateToken(authentication);

            // 在响应头中返回新令牌
            response.setHeader("X-New-Token", newToken);
            response.setHeader("Access-Control-Expose-Headers", "X-New-Token");

            // 设置认证上下文，使当前请求能够继续处理
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception refreshException) {
            log.error("令牌刷新失败", refreshException);
        }
    }
}