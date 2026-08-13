package com.nsp.server.security;

import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 商家身份上下文 — 从 JWT 解析当前登录商家 ID
 *
 * <p>商家登录时 JWT 的 sub 即为 merchant.id（见 MerchantServiceImpl 登录签发处），
 * 因此这里直接读取 SecurityContext principal，零 DB 查询。
 * 商家端接口不再接收前端传 merchantId，从源头杜绝越权传参。
 */
public final class MerchantContext {

    private MerchantContext() { /* 工具类禁止实例化 */ }

    /**
     * 获取当前登录商家的 merchantId
     *
     * @return 当前商家 ID（JWT sub）
     * @throws BizException 1001 未登录 / 1002 非商家角色
     */
    public static Long currentMerchantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, MessageConst.TOKEN_UNAUTHORIZED_MSG);
        }
        boolean isMerchant = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MERCHANT".equals(a.getAuthority()));
        if (!isMerchant) {
            throw new BizException(ErrorCode.FORBIDDEN, MessageConst.NOT_MERCHANT_OWNER_MSG);
        }
        return userId;
    }

}
