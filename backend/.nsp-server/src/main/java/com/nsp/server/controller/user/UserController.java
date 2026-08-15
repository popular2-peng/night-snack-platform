package com.nsp.server.controller.user;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.UserVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户信息接口
 */
@Tag(name = "用户信息", description = "查看和修改个人信息")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    @RequireRole("USER")
    public Result<UserVO> getInfo(@AuthenticationPrincipal Long userId) {
        UserVO vo = userService.getUserById(userId);
        return Result.ok(vo);
    }

}
