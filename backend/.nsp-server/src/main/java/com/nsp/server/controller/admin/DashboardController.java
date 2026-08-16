package com.nsp.server.controller.admin;

import com.nsp.common.result.Result;
import com.nsp.pojo.vo.AdminDashboardVO;
import com.nsp.pojo.vo.AdminRecentOrderVO;
import com.nsp.pojo.vo.OrderStatusStatVO;
import com.nsp.pojo.vo.TrendPointVO;
import com.nsp.server.annotation.RequireRole;
import com.nsp.server.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 平台管理后台 — 首页仪表盘
 */
@Tag(name = "仪表盘", description = "管理员：今日核心统计、近N日趋势、订单状态分布")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "核心统计（今日订单/营业额/新增用户/待审核 + 较昨日对比 + 更新时间）")
    @GetMapping("/dashboard/stats")
    public Result<AdminDashboardVO> stats() {
        return Result.ok(dashboardService.getStats());
    }

    @Operation(summary = "近 N 日注册用户趋势（每日新增用户数）")
    @GetMapping("/dashboard/user-trend")
    public Result<List<TrendPointVO>> userTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardService.getUserTrend(days));
    }

    @Operation(summary = "近 N 日商家入驻趋势（每日入驻申请数）")
    @GetMapping("/dashboard/application-trend")
    public Result<List<TrendPointVO>> applicationTrend(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardService.getApplicationTrend(days));
    }

    @Operation(summary = "订单状态分布（date 为空时全量，否则只统计该日）")
    @GetMapping("/dashboard/order-stats")
    public Result<List<OrderStatusStatVO>> orderStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dashboardService.getOrderStatusDistribution(date));
    }

    @Operation(summary = "最新订单")
    @GetMapping("/dashboard/recent-orders")
    public Result<List<AdminRecentOrderVO>> recentOrders(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dashboardService.getRecentOrders(limit));
    }

}
