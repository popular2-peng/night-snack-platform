package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.AdminDashboardVO;
import com.nsp.pojo.vo.AdminRecentOrderVO;
import com.nsp.pojo.vo.OrderStatusStatVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 平台管理后台 — 首页仪表盘
 */
@Tag(name = "仪表盘", description = "管理员：核心统计、订单分布、最新订单")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "核心统计卡片")
    @GetMapping("/dashboard/stats")
    public Result<AdminDashboardVO> stats() {
        return Result.ok(dashboardService.getStats());
    }

    @Operation(summary = "订单状态分布")
    @GetMapping("/dashboard/order-stats")
    public Result<List<OrderStatusStatVO>> orderStats() {
        return Result.ok(dashboardService.getOrderStatusDistribution());
    }

    @Operation(summary = "最新订单")
    @GetMapping("/dashboard/recent-orders")
    public Result<List<AdminRecentOrderVO>> recentOrders(
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.getRecentOrders(limit));
    }

}
