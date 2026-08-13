package com.nsp.common.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具类 — HS256 对称加密，用于用户/商家/管理员三端统一鉴权
 *
 * <p>Token 结构：
 * <ul>
 *   <li>{@code sub} — 用户 ID（商家登录时即为 merchant.id）</li>
 *   <li>{@code role} — 角色（USER / MERCHANT / ADMIN）</li>
 *   <li>{@code token_type} — Token 类型（ACCESS / REFRESH），防止刷新令牌被当作业务令牌使用</li>
 *   <li>{@code jti} — Token 唯一 ID，用于登出拉黑</li>
 *   <li>{@code iat} — 签发时间</li>
 *   <li>{@code exp} — 过期时间</li>
 * </ul>
 *
 * <p>安全配置：
 * <ul>
 *   <li>密钥从环境变量 {@code JWT_SECRET} 注入，禁止硬编码</li>
 *   <li>Access Token 有效期 2 小时</li>
 *   <li>Refresh Token 有效期 7 天</li>
 *   <li>密钥长度必须 ≥ 256 bits（32 字节），否则启动时抛出 IllegalStateException</li>
 * </ul>
 */
public class JwtUtil {

    /** Access Token 有效期：2 小时（毫秒） */
    private static final long ACCESS_EXPIRATION_MS = 2 * 60 * 60 * 1000L;

    /** Refresh Token 有效期：7 天（毫秒） */
    private static final long REFRESH_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L;

    /** JWT claims 中的角色字段名 */
    public static final String CLAIM_ROLE = "role";

    /** JWT claims 中的 Token 类型字段名（ACCESS / REFRESH，用于防止刷新令牌被当作业务令牌使用） */
    public static final String CLAIM_TOKEN_TYPE = "token_type";
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private final SecretKey secretKey;

    /**
     * 构造 JWT 工具实例
     *
     * @param secret JWT 签名密钥（HS256 要求 ≥ 256 bits，即 ≥ 32 字符）
     * @throws IllegalArgumentException 密钥长度不足
     */
    public JwtUtil(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT_SECRET 长度不能少于 32 字符（256 bits），当前长度: "
                    + (secret == null ? 0 : secret.length()));
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ──────────────────────────────────────
    // Token 签发
    // ──────────────────────────────────────

    /**
     * 签发 Access Token（2 小时有效）
     *
     * @param userId 用户 ID
     * @param role   角色（USER / MERCHANT / ADMIN）
     * @param jti    Token 唯一标识（用于登出拉黑）
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, String role, String jti) {
        return buildToken(userId, role, jti, ACCESS_EXPIRATION_MS, TOKEN_TYPE_ACCESS);
    }

    /**
     * 签发 Refresh Token（7 天有效）
     */
    public String generateRefreshToken(Long userId, String role, String jti) {
        return buildToken(userId, role, jti, REFRESH_EXPIRATION_MS, TOKEN_TYPE_REFRESH);
    }

    private String buildToken(Long userId, String role, String jti, long expirationMs, String tokenType) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .id(jti)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    // ──────────────────────────────────────
    // Token 解析与校验
    // ──────────────────────────────────────

    /**
     * 解析 Token，校验签名和过期时间
     *
     * @param token JWT 字符串
     * @return Claims（包含 sub、role、jti、iat、exp）
     * @throws JwtException Token 无效或过期
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户 ID
     */
    public Long getUserId(String token) {
        return Long.valueOf(parseToken(token).getSubject());
    }

    /**
     * 从 Token 中提取角色
     */
    public String getRole(String token) {
        return parseToken(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * 从 Token 中提取 JTI（Token 唯一标识）
     */
    public String getJti(String token) {
        return parseToken(token).getId();
    }

    /**
     * 判断 Token 是否有效（不抛异常即为有效）
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断 Token 是否已过期（仅过期返回 true；签名错误/格式错误等无效 Token 返回 false，
     * 由调用方结合 {@link #isValid} 区分「过期 1003」与「无效 1004」）
     */
    public boolean isExpired(String token) {
        try {
            parseToken(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 提取 Token 类型（ACCESS / REFRESH）
     *
     * @return 旧版本签发的 Token 无此 claim，返回 null（按 ACCESS 处理，平滑过渡）
     */
    public String getTokenType(String token) {
        return parseToken(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

}
