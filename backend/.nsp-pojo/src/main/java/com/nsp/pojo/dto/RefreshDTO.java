package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token 刷新请求体 — 用刷新令牌换取新的双令牌
 */
@Data
public class RefreshDTO {

    /** 刷新令牌（7 天有效期，登录/注册时签发） */
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;

}
