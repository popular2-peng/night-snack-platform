package com.nsp.server.security;

import com.nsp.common.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 — 每个请求都会经过此过滤器，从 Header 中提取并校验 JWT
 *
 * <p>认证失败原因写入 request attribute（见 {@link RestAuthenticationEntryPoint} 常量），
 * 由 EntryPoint 统一输出 401 + Result JSON（1001/1003/1004）。
 * 刷新令牌（token_type=REFRESH）禁止访问业务接口。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            // 无 Token：白名单接口继续匿名访问；受保护接口由 EntryPoint 返回 1001
            filterChain.doFilter(request, response);
            return;
        }

        // 校验失败：区分「过期 1003」与「无效 1004」
        if (!jwtTokenProvider.validateToken(token)) {
            request.setAttribute(RestAuthenticationEntryPoint.ERROR_ATTRIBUTE,
                    jwtTokenProvider.isTokenExpired(token)
                            ? RestAuthenticationEntryPoint.ERROR_EXPIRED
                            : RestAuthenticationEntryPoint.ERROR_INVALID);
            filterChain.doFilter(request, response);
            return;
        }

        // 刷新令牌禁止访问业务接口（旧 Token 无 token_type claim，视为 ACCESS，平滑过渡）
        if (JwtUtil.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.getTokenType(token))) {
            log.warn("刷新令牌被用于业务接口（已拒绝）: path={}", request.getRequestURI());
            request.setAttribute(RestAuthenticationEntryPoint.ERROR_ATTRIBUTE,
                    RestAuthenticationEntryPoint.ERROR_REFRESH_TYPE);
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtTokenProvider.getUserId(token);
        String role = jwtTokenProvider.getRole(token);

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT 认证成功: userId={}, role={}", userId, role);

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

}
