package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.BanMerchantDTO;
import com.nsp.pojo.vo.MerchantDetailVO;
import com.nsp.pojo.vo.MerchantStatsVO;
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
 * 商家管理接口 — 管理员对商家的详细操作
 */
@Tag(name = "商家管理", description = "管理员：商家详情、封禁/解封、暂停/恢复、操作日志")
@RestController
@RequestMapping("/api/admin/merchant")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class MerchantAdminController {

    private final MerchantAdminService merchantAdminService;

    @Operation(summary = "商家统计概览")
    @GetMapping("/stats")
    public Result<MerchantStatsVO> stats() {
        return Result.ok(merchantAdminService.getStats());
    }

    @Operation(summary = "商家详情")
    @GetMapping("/{id}/detail")
    public Result<MerchantDetailVO> detail(@PathVariable Long id) {
        return Result.ok(merchantAdminService.getDetail(id));
    }

    @Operation(summary = "暂停营业")
    @PutMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id,
                              @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.pause(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "恢复营业")
    @PutMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id,
                               @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.resume(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "封禁商家")
    @PutMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable Long id,
                            @AuthenticationPrincipal Long operatorId,
                            @Valid @RequestBody BanMerchantDTO dto) {
        merchantAdminService.ban(id, operatorId, dto);
        return Result.ok();
    }

    @Operation(summary = "解封商家")
    @PutMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable Long id,
                              @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.unban(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "删除商家（软删）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal Long operatorId) {
        merchantAdminService.softDelete(id, operatorId);
        return Result.ok();
    }

    @Operation(summary = "操作日志列表")
    @GetMapping("/{id}/logs")
    public Result<List<Map<String, Object>>> logs(@PathVariable Long id) {
        return Result.ok(merchantAdminService.getOperationLogs(id));
    }

}
