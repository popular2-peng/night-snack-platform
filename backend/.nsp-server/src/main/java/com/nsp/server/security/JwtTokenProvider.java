package com.nsp.server.security;

import com.nsp.common.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JWT Token 提供者 — 封装 JwtUtil，统一管理 Token 签发逻辑
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtUtil jwtUtil;

    /**
     * 签发 Access Token
     */
    public String createAccessToken(Long userId, String role) {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateAccessToken(userId, role, jti);
        log.info("签发 AccessToken: userId={}, role={}, jti={}", userId, role, jti);
        return token;
    }

    /**
     * 签发 Refresh Token
     */
    public String createRefreshToken(Long userId, String role) {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateRefreshToken(userId, role, jti);
        log.info("签发 RefreshToken: userId={}, jti={}", userId, jti);
        return token;
    }

    public boolean validateToken(String token) {
        return jwtUtil.isValid(token);
    }

    public Long getUserId(String token) {
        return jwtUtil.getUserId(token);
    }

    public String getRole(String token) {
        return jwtUtil.getRole(token);
    }

    public String getJti(String token) {
        return jwtUtil.getJti(token);
    }

    /**
     * 提取 Token 类型（ACCESS / REFRESH；旧 Token 无此 claim 时返回 null）
     */
    public String getTokenType(String token) {
        return jwtUtil.getTokenType(token);
    }

    /** 仅校验是否过期（只捕获 ExpiredJwtException，供过滤器区分 1003/1004） */
    public boolean isTokenExpired(String token) {
        return jwtUtil.isExpired(token);
    }

}
