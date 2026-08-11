package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.ApproveApplicationDTO;
import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.dto.BatchIdsDTO;
import com.nsp.pojo.dto.BatchRejectDTO;
import com.nsp.pojo.dto.RejectApplicationDTO;
import com.nsp.pojo.vo.AdminApplicationVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.ApplicationReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 平台管理后台 — 商家入驻审核
 */
@Tag(name = "入驻审核", description = "管理员：商家入驻申请列表、审核通过/拒绝、申请详情")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class ApplicationReviewController {

    private final ApplicationReviewService applicationReviewService;

    @Operation(summary = "商家入驻申请列表（支持关键词+状态+品类+日期范围筛选）")
    @GetMapping("/merchant-application/pending")
    public Result<PageVO<AdminApplicationVO>> listApplications(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        return Result.ok(applicationReviewService.listApplications(page, size, keyword, status, categoryId, dateStart, dateEnd));
    }

    @Operation(summary = "通过商家入驻申请")
    @PutMapping("/merchant-application/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @AuthenticationPrincipal Long reviewerId) {
        applicationReviewService.approveApplication(id, reviewerId);
        return Result.ok();
    }

    @Operation(summary = "拒绝商家入驻申请")
    @PutMapping("/merchant-application/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @AuthenticationPrincipal Long reviewerId,
                               @RequestBody AuditDTO dto) {
        applicationReviewService.rejectApplication(id, reviewerId, dto);
        return Result.ok();
    }

    @Operation(summary = "入驻申请详情")
    @GetMapping("/merchant-application/{id}/detail")
    public Result<AdminApplicationVO> applicationDetail(@PathVariable Long id) {
        return Result.ok(applicationReviewService.getApplicationDetail(id));
    }

    @Operation(summary = "批量通过入驻申请（已审核的自动跳过，默认营业中+配送半径5000）")
    @PostMapping("/merchant-application/batch-approve")
    public Result<BatchResultVO> batchApprove(@Valid @RequestBody BatchIdsDTO dto,
                                              @AuthenticationPrincipal Long reviewerId) {
        return Result.ok(applicationReviewService.batchApprove(dto.getIds(), reviewerId));
    }

    @Operation(summary = "批量拒绝入驻申请（统一拒绝原因，已审核的自动跳过）")
    @PostMapping("/merchant-application/batch-reject")
    public Result<BatchResultVO> batchReject(@Valid @RequestBody BatchRejectDTO dto,
                                             @AuthenticationPrincipal Long reviewerId) {
        return Result.ok(applicationReviewService.batchReject(dto.getIds(), reviewerId, dto.getRejectReason()));
    }

    @Operation(summary = "通过入驻申请（V2，含初始营业状态/配送半径/审核备注）")
    @PostMapping("/application/{id}/approve")
    public Result<Void> approveV2(@PathVariable Long id,
                                  @AuthenticationPrincipal Long reviewerId,
                                  @RequestBody ApproveApplicationDTO dto) {
        applicationReviewService.approveApplicationV2(id, reviewerId, dto);
        return Result.ok();
    }

    @Operation(summary = "拒绝入驻申请（V2，拒绝原因必填）")
    @PostMapping("/application/{id}/reject")
    public Result<Void> rejectV2(@PathVariable Long id,
                                  @AuthenticationPrincipal Long reviewerId,
                                  @Valid @RequestBody RejectApplicationDTO dto) {
        applicationReviewService.rejectApplicationV2(id, reviewerId, dto);
        return Result.ok();
    }

}
