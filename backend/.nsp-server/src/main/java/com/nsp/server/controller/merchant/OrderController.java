package com.nsp.server.controller.merchant;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.OrderListVO;
import com.nsp.pojo.vo.OrderVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.security.MerchantContext;
import com.nsp.server.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商家端订单接口
 *
 * <p>商家身份由 JWT 解析（sub = merchant.id），不再接收前端传 merchantId；
 * 订单归属校验仍在 Service 层（3301），作为纵深防御保留。
 */
@Tag(name = "商家订单", description = "接单、制作、完成、拒单")
@RestController("merchantOrderController")
@RequestMapping("/api/merchant/order")
@RequiredArgsConstructor
@RequireRole("MERCHANT")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "店铺订单列表（当前登录商家，支持状态筛选与下单日期范围，含商品明细与地址快照）")
    @GetMapping
    public Result<PageVO<OrderListVO>> list(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String dateStart,
                                             @RequestParam(required = false) String dateEnd) {
        Long merchantId = MerchantContext.currentMerchantId();
        return Result.ok(orderService.getMerchantOrders(merchantId, merchantId, page, size, status, dateStart, dateEnd));
    }

    @Operation(summary = "订单状态分布统计（tab 角标：status → 数量，可选下单日期范围，含边界当天）")
    @GetMapping("/stats")
    public Result<java.util.Map<String, Long>> stats(@RequestParam(required = false) String dateStart,
                                                     @RequestParam(required = false) String dateEnd) {
        Long merchantId = MerchantContext.currentMerchantId();
        return Result.ok(orderService.getMerchantOrderStats(merchantId, merchantId, dateStart, dateEnd));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getById(@PathVariable Long id) {
        return Result.ok(orderService.getOrderDetail(id, MerchantContext.currentMerchantId(), "MERCHANT"));
    }

    @Operation(summary = "接单")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        Long merchantId = MerchantContext.currentMerchantId();
        orderService.acceptOrder(id, merchantId, merchantId);
        return Result.ok();
    }

    @Operation(summary = "开始制作")
    @PutMapping("/{id}/cooking")
    public Result<Void> cooking(@PathVariable Long id) {
        Long merchantId = MerchantContext.currentMerchantId();
        orderService.startCooking(id, merchantId, merchantId);
        return Result.ok();
    }

    @Operation(summary = "完成制作")
    @PutMapping("/{id}/complete")
    public Result<Void> complete(@PathVariable Long id) {
        Long merchantId = MerchantContext.currentMerchantId();
        orderService.completeOrder(id, merchantId, merchantId);
        return Result.ok();
    }

    @Operation(summary = "拒单（已支付订单触发自动退款）")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam String reason) {
        Long merchantId = MerchantContext.currentMerchantId();
        orderService.rejectOrder(id, merchantId, merchantId, reason);
        return Result.ok();
    }

}
