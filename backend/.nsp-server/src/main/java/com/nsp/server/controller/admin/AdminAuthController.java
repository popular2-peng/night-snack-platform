package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.vo.LoginVO;
import com.nsp.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端认证 — 管理员登录
 *
 * <p>专用登录接口，仅查 admin 表；用户登录走 /api/user/login，商家登录走 /api/merchant/login。
 * SecurityConfig 已放行（匿名可访问）。
 */
@Tag(name = "管理员认证", description = "管理员登录（专用接口，仅查 admin 表）")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserService userService;

    @Operation(summary = "管理员登录（用户名 + 密码）")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.adminLogin(dto));
    }

}
