package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.AdminUserVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 平台管理后台 — 用户管理（顾客 user 表）
 */
@Tag(name = "用户管理", description = "管理员：用户列表、封禁/解封")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageVO<AdminUserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(userManagementService.listUsers(page, size, keyword));
    }

    @Operation(summary = "封禁用户")
    @PutMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        userManagementService.disableUser(id);
        return Result.ok();
    }

    @Operation(summary = "解封用户")
    @PutMapping("/users/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        userManagementService.enableUser(id);
        return Result.ok();
    }

}
