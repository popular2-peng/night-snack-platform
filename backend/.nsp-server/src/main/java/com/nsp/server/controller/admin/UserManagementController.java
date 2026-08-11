package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.BanUserDTO;
import com.nsp.pojo.dto.BatchIdsDTO;
import com.nsp.pojo.vo.AdminUserVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.pojo.vo.UserDetailVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 平台管理后台 — 用户管理（顾客 user 表）
 */
@Tag(name = "用户管理", description = "管理员：用户列表、详情、封禁/解封、重置密码")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @Operation(summary = "用户列表（支持关键词/状态/注册时间筛选）")
    @GetMapping("/users")
    public Result<PageVO<AdminUserVO>> listUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "搜索：用户名/手机号/昵称") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选：ACTIVE/DISABLED") @RequestParam(required = false) String status,
            @Parameter(description = "注册起始日期（yyyy-MM-dd）") @RequestParam(required = false) String dateStart,
            @Parameter(description = "注册截止日期（yyyy-MM-dd）") @RequestParam(required = false) String dateEnd) {
        return Result.ok(userManagementService.listUsers(page, size, keyword, status, dateStart, dateEnd));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/users/{id}/detail")
    public Result<UserDetailVO> detail(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return Result.ok(userManagementService.getUserDetail(id));
    }

    @Operation(summary = "封禁用户（含原因+时长）")
    @PutMapping("/users/{id}/ban")
    public Result<Void> banUser(
            @Parameter(description = "用户 ID") @PathVariable Long id,
            @AuthenticationPrincipal Long operatorId,
            @Valid @RequestBody BanUserDTO dto) {
        userManagementService.banUser(id, operatorId, dto);
        return Result.ok();
    }

    @Operation(summary = "解封用户")
    @PutMapping("/users/{id}/unban")
    public Result<Void> unbanUser(
            @Parameter(description = "用户 ID") @PathVariable Long id,
            @AuthenticationPrincipal Long operatorId) {
        userManagementService.unbanUser(id, operatorId);
        return Result.ok();
    }

    // ── 兼容旧前端：disable / enable 委托到新 ban / unban ──

    @Operation(summary = "封禁用户（旧版兼容，无原因+永久）", deprecated = true)
    @PutMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        BanUserDTO dto = new BanUserDTO();
        dto.setReason("管理员操作");
        userManagementService.banUser(id, 0L, dto);
        return Result.ok();
    }

    @Operation(summary = "解封用户（旧版兼容）", deprecated = true)
    @PutMapping("/users/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        userManagementService.unbanUser(id, 0L);
        return Result.ok();
    }

    @Operation(summary = "重置密码（返回新密码）")
    @PutMapping("/users/{id}/reset-password")
    public Result<String> resetPassword(
            @Parameter(description = "用户 ID") @PathVariable Long id,
            @AuthenticationPrincipal Long operatorId) {
        String newPassword = userManagementService.resetPassword(id, operatorId);
        return Result.ok(newPassword);
    }

    @Operation(summary = "删除用户（物理删除 + 级联清理订单/地址，启用账号不可删除）")
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(
            @Parameter(description = "用户 ID") @PathVariable Long id,
            @AuthenticationPrincipal Long operatorId) {
        userManagementService.deleteUser(id, operatorId);
        return Result.ok();
    }

    // ── 批量操作（封禁需选时长，不提供批量封禁） ──

    @Operation(summary = "批量启用（未封禁/已删除的自动跳过）")
    @PostMapping("/users/batch-unban")
    public Result<BatchResultVO> batchUnban(@Valid @RequestBody BatchIdsDTO dto,
                                            @AuthenticationPrincipal Long operatorId) {
        return Result.ok(userManagementService.batchUnban(dto.getIds(), operatorId));
    }

    @Operation(summary = "批量删除（软删，启用账号不可删除，仅封禁账号可删）")
    @PostMapping("/users/batch-delete")
    public Result<BatchResultVO> batchDelete(@Valid @RequestBody BatchIdsDTO dto,
                                             @AuthenticationPrincipal Long operatorId) {
        return Result.ok(userManagementService.batchDelete(dto.getIds(), operatorId));
    }

    @Operation(summary = "操作日志列表")
    @GetMapping("/users/{id}/logs")
    public Result<List<Map<String, Object>>> logs(@Parameter(description = "用户 ID") @PathVariable Long id) {
        return Result.ok(userManagementService.getOperationLogs(id));
    }

}
