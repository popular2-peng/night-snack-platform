package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.ApproveApplicationDTO;
import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.dto.RejectApplicationDTO;
import com.nsp.pojo.vo.*;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台管理后台接口
 */
@Tag(name = "平台管理", description = "管理员：商家审核、用户管理、商家管理、数据统计")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminController {

    private final AdminService adminService;

    // ──────────────────────────────────────
    // 首页仪表盘（拆分）
    // ──────────────────────────────────────

    @Operation(summary = "核心统计卡片")
    @GetMapping("/dashboard/stats")
    public Result<AdminDashboardVO> stats() {
        return Result.ok(adminService.getStats());
    }

    @Operation(summary = "订单状态分布")
    @GetMapping("/dashboard/order-stats")
    public Result<List<OrderStatusStatVO>> orderStats() {
        return Result.ok(adminService.getOrderStatusDistribution());
    }

    @Operation(summary = "最新订单")
    @GetMapping("/dashboard/recent-orders")
    public Result<List<AdminRecentOrderVO>> recentOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(adminService.getRecentOrders(limit));
    }

    // ──────────────────────────────────────
    // 商家入驻审核
    // ──────────────────────────────────────

    @Operation(summary = "待审核商家申请列表")
    @GetMapping("/merchant-application/pending")
    public Result<PageVO<AdminApplicationVO>> listApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminService.listApplications(page, size));
    }

    @Operation(summary = "通过商家入驻申请")
    @PutMapping("/merchant-application/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @AuthenticationPrincipal Long reviewerId) {
        adminService.approveApplication(id, reviewerId);
        return Result.ok();
    }

    @Operation(summary = "拒绝商家入驻申请")
    @PutMapping("/merchant-application/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @AuthenticationPrincipal Long reviewerId,
                               @RequestBody AuditDTO dto) {
        adminService.rejectApplication(id, reviewerId, dto);
        return Result.ok();
    }

    @Operation(summary = "入驻申请详情")
    @GetMapping("/merchant-application/{id}/detail")
    public Result<AdminApplicationVO> applicationDetail(@PathVariable Long id) {
        return Result.ok(adminService.getApplicationDetail(id));
    }

    @Operation(summary = "通过入驻申请（V2，含初始营业状态/配送半径/审核备注）")
    @PostMapping("/application/{id}/approve")
    public Result<Void> approveV2(@PathVariable Long id,
                                  @AuthenticationPrincipal Long reviewerId,
                                  @RequestBody ApproveApplicationDTO dto) {
        adminService.approveApplicationV2(id, reviewerId, dto);
        return Result.ok();
    }

    @Operation(summary = "拒绝入驻申请（V2，拒绝原因必填）")
    @PostMapping("/application/{id}/reject")
    public Result<Void> rejectV2(@PathVariable Long id,
                                  @AuthenticationPrincipal Long reviewerId,
                                  @Valid @RequestBody RejectApplicationDTO dto) {
        adminService.rejectApplicationV2(id, reviewerId, dto);
        return Result.ok();
    }

    // ──────────────────────────────────────
    // 用户管理
    // ──────────────────────────────────────

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<PageVO<AdminUserVO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(adminService.listUsers(page, size, keyword));
    }

    @Operation(summary = "封禁用户")
    @PutMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id) {
        adminService.disableUser(id);
        return Result.ok();
    }

    @Operation(summary = "解封用户")
    @PutMapping("/users/{id}/enable")
    public Result<Void> enableUser(@PathVariable Long id) {
        adminService.enableUser(id);
        return Result.ok();
    }

    // ──────────────────────────────────────
    // 商家管理
    // ──────────────────────────────────────

    @Operation(summary = "商家列表")
    @GetMapping("/merchants")
    public Result<PageVO<AdminMerchantVO>> listMerchants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(adminService.listMerchants(page, size, keyword, status));
    }

    @Operation(summary = "封禁商家")
    @PutMapping("/merchants/{id}/suspend")
    public Result<Void> suspendMerchant(@PathVariable Long id) {
        adminService.suspendMerchant(id);
        return Result.ok();
    }

    @Operation(summary = "解封商家")
    @PutMapping("/merchants/{id}/activate")
    public Result<Void> activateMerchant(@PathVariable Long id) {
        adminService.activateMerchant(id);
        return Result.ok();
    }

}
