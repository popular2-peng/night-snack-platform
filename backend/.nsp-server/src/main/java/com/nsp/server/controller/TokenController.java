package com.nsp.server.controller;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.RefreshDTO;
import com.nsp.pojo.vo.RefreshTokenVO;
import com.nsp.server.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token 刷新 — 三端（用户/商家/管理员）通用接口
 *
 * <p>放 controller 顶层（与 UploadController 同理）：
 * 刷新令牌自身即凭证（免登录白名单），角色从令牌解析，不挂 @RequireRole。
 */
@Tag(name = "Token 刷新", description = "三端通用：刷新令牌换取新双令牌")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    @Operation(summary = "刷新 Token（refresh token 换取新 access + refresh，双令牌轮换）")
    @PostMapping("/refresh")
    public Result<RefreshTokenVO> refresh(@Valid @RequestBody RefreshDTO dto) {
        return Result.ok(tokenService.refresh(dto.getRefreshToken()));
    }

}
