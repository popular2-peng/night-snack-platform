package com.nsp.server.controller.user;

import com.nsp.common.result.Result;
import com.nsp.pojo.dto.OrderCreateDTO;
import com.nsp.pojo.vo.OrderListVO;
import com.nsp.pojo.vo.OrderVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端订单接口
 */
@Tag(name = "用户订单", description = "下单、查单、取消")
@RestController("userOrderController")
@RequestMapping("/api/user/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单（下单）")
    @PostMapping
    @RequireRole("USER")
    public Result<OrderVO> create(@Valid @RequestBody OrderCreateDTO dto,
                                  @AuthenticationPrincipal Long userId) {
        return Result.ok(orderService.createOrder(userId, dto));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping
    @RequireRole("USER")
    public Result<PageVO<OrderListVO>> list(@AuthenticationPrincipal Long userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return Result.ok(orderService.getUserOrders(userId, page, size));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    @RequireRole("USER")
    public Result<OrderVO> getById(@PathVariable Long id,
                                   @AuthenticationPrincipal Long userId) {
        return Result.ok(orderService.getOrderDetail(id, userId, "USER"));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    @RequireRole("USER")
    public Result<Void> cancel(@PathVariable Long id,
                               @AuthenticationPrincipal Long userId) {
        orderService.cancelOrder(id, userId);
        return Result.ok();
    }

}
