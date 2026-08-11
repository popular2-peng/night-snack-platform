package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.BanMerchantDTO;
import com.nsp.pojo.dto.BatchIdsDTO;
import com.nsp.pojo.vo.AdminMerchantVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.MerchantDetailVO;
import com.nsp.pojo.vo.MerchantStatsVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.MerchantAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 平台管理后台 — 商家管理（列表 + 详情 + 封禁/解封/暂停/恢复/删除）
 */
@Tag(name = "商家管理", description = "管理员：商家列表、详情、封禁/解封、暂停/恢复、操作日志")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class MerchantManagementController {

    private final MerchantAdminService merchantAdminService;

    // ── 商家列表（分页+筛选） ──

    @Operation(summary = "商家列表")
    @GetMapping("/merchants")
    public Result<PageVO<AdminMerchantVO>> listMerchants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        return Result.ok(merchantAdminService.listMerchants(page, size, keyword, status, categoryId, dateStart, dateEnd));
    }

    @Operation(summary = "封禁商家（简易版，仅设状态）")
    @PutMapping("/merchants/{id}/suspend")
    public Result<Void> suspendMerchant(@PathVariable Long id) {
        merchantAdminService.suspendMerchant(id);
        return Result.ok();
    }

    @Operation(summary = "解封商家（简易版）")
    @PutMapping("/merchants/{id}/activate")
    public Result<Void> activateMerchant(@PathVariable Long id) {
        merchantAdminService.activateMerchant(id);
        return Result.ok();
    }

    // ── 商家统计 ──

    @Operation(summary = "商家统计概览")
    @GetMapping("/merchant/stats")
    public Result<MerchantStatsVO> stats() {
        return Result.ok(merchantAdminService.getStats());
    }

    // ── 商家详情与操作 ──

    @Operation(summary = "商家详情")
    @GetMapping("/merchant/{id}/detail")
    public Result<MerchantDetailVO> detail(@PathVariable Long id) {
        return Result.ok(merchantAdminService.getDetail(id));
    }

    @Operation(summary = "暂停营业")
    @PutMapping("/merchant/{id}/pause")
    public Result<Void> pause(@PathVariable Long id,
                              @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.pause(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "恢复营业")
    @PutMapping("/merchant/{id}/resume")
    public Result<Void> resume(@PathVariable Long id,
                               @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.resume(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "封禁商家（含原因）")
    @PutMapping("/merchant/{id}/ban")
    public Result<Void> ban(@PathVariable Long id,
                            @AuthenticationPrincipal Long operatorId,
                            @Valid @RequestBody BanMerchantDTO dto) {
        merchantAdminService.ban(id, operatorId, dto);
        return Result.ok();
    }

    @Operation(summary = "解封商家")
    @PutMapping("/merchant/{id}/unban")
    public Result<Void> unban(@PathVariable Long id,
                              @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.unban(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "删除商家（仅封禁可删，级联清理订单/商品/分类）")
    @DeleteMapping("/merchant/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.softDelete(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "批量删除商家（级联清理订单/商品等，未封禁自动跳过）")
    @PostMapping("/merchant/batch-delete")
    public Result<BatchResultVO> batchDelete(@Valid @RequestBody BatchIdsDTO dto) {
        return Result.ok(merchantAdminService.batchDelete(dto.getIds()));
    }

    @Operation(summary = "操作日志列表")
    @GetMapping("/merchant/{id}/logs")
    public Result<List<Map<String, Object>>> logs(@PathVariable Long id) {
        return Result.ok(merchantAdminService.getOperationLogs(id));
    }

}
