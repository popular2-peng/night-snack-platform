package com.nsp.server.controller.user;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.LoginDTO;
import com.nsp.pojo.dto.RegisterDTO;
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
 * 用户认证接口 — 登录/注册（白名单，无需 Token）
 */
@Tag(name = "用户认证", description = "登录、注册")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto) {
        LoginVO vo = userService.register(dto);
        return Result.ok(vo);
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO vo = userService.login(dto);
        return Result.ok(vo);
    }

}
