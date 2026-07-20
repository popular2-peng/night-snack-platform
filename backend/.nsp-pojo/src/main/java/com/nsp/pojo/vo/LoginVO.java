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

    /** 用户名 */
    private String username;

    /** 角色：USER / MERCHANT / ADMIN */
    private String role;

    /** Access Token（2 小时有效） */
    private String accessToken;

    /** Refresh Token（7 天有效） */
    private String refreshToken;

}
