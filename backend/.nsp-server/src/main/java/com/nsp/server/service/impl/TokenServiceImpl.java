package com.nsp.server.service.impl;

import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.common.jwt.JwtUtil;
import com.nsp.pojo.entity.Admin;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.RefreshTokenVO;
import com.nsp.server.mapper.AdminMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.security.JwtTokenProvider;
import com.nsp.server.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Token 刷新业务实现 — 刷新令牌换取新双令牌（轮换）
 *
 * <p>安全要点：
 * <ul>
 *   <li>仅接受 token_type=REFRESH 的令牌（access 令牌不可用于刷新）</li>
 *   <li>按角色校验账号状态，被封禁/审核中的账号不允许续期</li>
 *   <li>每次刷新同时签发新的 access 与 refresh（旧 refresh 到期前仍可用，MVP 取舍见 api.md）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenServiceImpl.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final AdminMapper adminMapper;

    @Override
    public RefreshTokenVO refresh(String refreshToken) {
        // 1. 校验令牌：过期 1003 / 无效 1004
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            if (jwtTokenProvider.isTokenExpired(refreshToken)) {
                throw new BizException(ErrorCode.TOKEN_EXPIRED, MessageConst.TOKEN_EXPIRED_MSG);
            }
            throw new BizException(ErrorCode.TOKEN_INVALID, MessageConst.TOKEN_REFRESH_INVALID_MSG);
        }

        // 2. 必须是 REFRESH 型令牌（access 令牌 / 无类型旧令牌不可用于刷新）
        if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new BizException(ErrorCode.TOKEN_INVALID, MessageConst.TOKEN_REFRESH_INVALID_MSG);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String role = jwtTokenProvider.getRole(refreshToken);

        // 3. 按角色校验账号状态（商家登录时 JWT 的 sub 即 merchant.id）
        switch (role) {
            case "USER" -> {
                User user = userMapper.selectById(userId);
                if (user == null) {
                    throw new BizException(ErrorCode.TOKEN_INVALID, MessageConst.TOKEN_REFRESH_INVALID_MSG);
                }
                if ("DISABLED".equals(user.getStatus())) {
                    throw new BizException(ErrorCode.USER_ALREADY_BANNED, MessageConst.TOKEN_REFRESH_ACCOUNT_BANNED_MSG);
                }
            }
            case "MERCHANT" -> {
                Merchant merchant = merchantMapper.selectById(userId);
                if (merchant == null) {
                    throw new BizException(ErrorCode.TOKEN_INVALID, MessageConst.TOKEN_REFRESH_INVALID_MSG);
                }
                if ("SUSPENDED".equals(merchant.getStatus())) {
                    throw new BizException(ErrorCode.MERCHANT_ALREADY_BANNED, MessageConst.TOKEN_REFRESH_ACCOUNT_BANNED_MSG);
                }
                if ("PENDING".equals(merchant.getStatus())) {
                    throw new BizException(ErrorCode.MERCHANT_NOT_APPROVED, MessageConst.MERCHANT_NOT_APPROVED_MSG);
                }
            }
            case "ADMIN" -> {
                Admin admin = adminMapper.selectById(userId);
                if (admin == null) {
                    throw new BizException(ErrorCode.TOKEN_INVALID, MessageConst.TOKEN_REFRESH_INVALID_MSG);
                }
                if ("DISABLED".equals(admin.getStatus())) {
                    throw new BizException(ErrorCode.USER_ALREADY_BANNED, MessageConst.TOKEN_REFRESH_ACCOUNT_BANNED_MSG);
                }
            }
            default -> throw new BizException(ErrorCode.TOKEN_INVALID, MessageConst.TOKEN_REFRESH_INVALID_MSG);
        }

        // 4. 签发新双令牌（轮换）
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, role);
        log.info("Token 刷新成功: userId={}, role={}", userId, role);

        return RefreshTokenVO.builder()
                .userId(userId)
                .role(role)
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

}
