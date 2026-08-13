package com.nsp.server.service;

import com.nsp.pojo.vo.RefreshTokenVO;

/**
 * Token 刷新业务接口 — 三端（用户/商家/管理员）通用
 */
public interface TokenService {

    /**
     * 用刷新令牌换取新的双令牌（access + refresh 同时轮换）
     *
     * <p>校验：刷新令牌有效且为 REFRESH 型；按角色校验账号状态
     * （封禁/审核中的账号不允许续期，防止被封禁账号无限续期）。
     *
     * @param refreshToken 刷新令牌
     * @return 新签发的双令牌
     */
    RefreshTokenVO refresh(String refreshToken);

}
