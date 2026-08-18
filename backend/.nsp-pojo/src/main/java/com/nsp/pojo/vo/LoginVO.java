package com.nsp.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/注册成功响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** 用户 ID */
    private Long userId;

    /**
     * 展示名：USER → 后端生成的用户名（普通用户+随机串）；MERCHANT → 店铺名称；ADMIN → 登录用户名
     */
    private String username;

    /** 角色：USER / MERCHANT / ADMIN */
    private String role;

    /** Access Token（2 小时有效） */
    private String accessToken;

    /** Refresh Token（7 天有效） */
    private String refreshToken;

}
