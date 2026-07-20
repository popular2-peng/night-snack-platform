package com.nsp.server.security;

import com.nsp.common.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * JWT Token 提供者 — 封装 JwtUtil，统一管理 Token 签发逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtUtil jwtUtil;

    /**
     * 签发 Access Token
     *
     * @param userId 用户 ID
     * @param role   角色（USER / MERCHANT / ADMIN）
     * @return JWT Access Token
     */
    public String createAccessToken(Long userId, String role) {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateAccessToken(userId, role, jti);
        log.info("签发 AccessToken: userId={}, role={}, jti={}", userId, role, jti);
        return token;
    }

    /**
     * 签发 Refresh Token
     *
     * @param userId 用户 ID
     * @param role   角色
     * @return JWT Refresh Token
     */
    public String createRefreshToken(Long userId, String role) {
        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.generateRefreshToken(userId, role, jti);
        log.info("签发 RefreshToken: userId={}, jti={}", userId, jti);
        return token;
    }

    /**
     * 校验 Token
     */
    public boolean validateToken(String token) {
        return jwtUtil.isValid(token);
    }

    /**
     * 从 Token 提取用户 ID
     */
    public Long getUserId(String token) {
        return jwtUtil.getUserId(token);
    }

    /**
     * 从 Token 提取角色
     */
    public String getRole(String token) {
        return jwtUtil.getRole(token);
    }

    /**
     * 从 Token 提取 JTI
     */
    public String getJti(String token) {
        return jwtUtil.getJti(token);
    }

}
