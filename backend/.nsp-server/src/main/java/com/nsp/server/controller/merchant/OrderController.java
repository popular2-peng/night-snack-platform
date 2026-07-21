package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.OrderListVO;
import com.nsp.pojo.vo.OrderVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 商家端订单接口
 */
@Tag(name = "商家订单", description = "接单、制作、完成、拒单")
@RestController
@RequestMapping("/api/merchant/order")
@RequiredArgsConstructor
@RequireRole("MERCHANT")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "店铺订单列表")
    @GetMapping
    public Result<PageVO<OrderListVO>> list(@RequestParam Long merchantId,
                                             @AuthenticationPrincipal Long userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return Result.ok(orderService.getMerchantOrders(merchantId, userId, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id,
                                   @AuthenticationPrincipal Long userId) {
        return Result.ok(orderService.getOrderDetail(id, userId, "MERCHANT"));
    }

    @Operation(summary = "接单")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id,
                               @RequestParam Long merchantId,
                               @AuthenticationPrincipal Long userId) {
        orderService.acceptOrder(id, merchantId, userId);
        return Result.ok();
    }

    @Operation(summary = "开始制作")
    @PutMapping("/{id}/cooking")
    public Result<Void> cooking(@PathVariable Long id,
                                @RequestParam Long merchantId,
                                @AuthenticationPrincipal Long userId) {
        orderService.startCooking(id, merchantId, userId);
        return Result.ok();
    }

    @Operation(summary = "完成制作")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id,
                                 @RequestParam Long merchantId,
                                 @AuthenticationPrincipal Long userId) {
        orderService.completeOrder(id, merchantId, userId);
        return Result.ok();
    }

    @Operation(summary = "拒单")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam Long merchantId,
                               @AuthenticationPrincipal Long userId,
                               @RequestParam String reason) {
        orderService.rejectOrder(id, merchantId, userId, reason);
        return Result.ok();
    }

}
