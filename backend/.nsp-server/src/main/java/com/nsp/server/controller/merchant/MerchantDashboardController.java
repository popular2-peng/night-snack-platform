package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.MerchantDashboardVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.security.MerchantContext;
import com.nsp.server.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家工作台 — 首页经营概况
 *
 * <p>商家身份由 JWT 解析（sub = merchant.id），不再接收前端传 merchantId。
 */
@Tag(name = "商家工作台", description = "今日订单/营业额/待接单/在售商品/今日状态分布")
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@RequireRole("MERCHANT")
public class MerchantDashboardController {

    private final OrderService orderService;

    @Operation(summary = "工作台首页统计")
    @GetMapping("/dashboard")
    public Result<MerchantDashboardVO> dashboard() {
        Long merchantId = MerchantContext.currentMerchantId();
        return Result.ok(orderService.getMerchantDashboard(merchantId, merchantId));
    }

}
