package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * Token 刷新响应 — 双 Token 轮换（access 2 小时 / refresh 7 天，均为新签发）
 */
@Data
@Builder
public class RefreshTokenVO {

    /** 用户 ID（商家登录时即为 merchant.id） */
    private Long userId;

    /** 角色：USER / MERCHANT / ADMIN */
    private String role;

    /** 新访问令牌（有效期 2 小时） */
    private String accessToken;

    /** 新刷新令牌（有效期 7 天，旧刷新令牌同时作废语义，见 api.md） */
    private String refreshToken;

}
