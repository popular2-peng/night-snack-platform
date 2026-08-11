package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.AdminOrderVO;
import com.nsp.pojo.vo.OrderVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 平台管理后台 — 订单管理（管理员可查看平台全部订单，不做状态流转操作）
 */
@Tag(name = "订单管理", description = "管理员：全部订单列表、订单详情")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "订单列表（支持订单号/用户名/商家名关键词、状态、下单日期筛选）")
    @GetMapping("/orders")
    public Result<PageVO<AdminOrderVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "搜索：订单号/用户名/商家名") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选：PENDING_PAY/PAID/ACCEPTED/COOKING/COMPLETED/CANCELLED/REFUNDING/REFUNDED")
            @RequestParam(required = false) String status,
            @Parameter(description = "下单起始日期（yyyy-MM-dd）") @RequestParam(required = false) String dateStart,
            @Parameter(description = "下单截止日期（yyyy-MM-dd）") @RequestParam(required = false) String dateEnd) {
        return Result.ok(orderService.getAdminOrders(page, size, keyword, status, dateStart, dateEnd));
    }

    @Operation(summary = "订单详情（含明细与状态流转日志）")
    @GetMapping("/orders/{id}")
    public Result<OrderVO> detail(@Parameter(description = "订单 ID") @PathVariable Long id) {
        return Result.ok(orderService.getOrderDetail(id, null, "ADMIN"));
    }

}
