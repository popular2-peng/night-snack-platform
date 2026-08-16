package com.nsp.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.enums.OrderStatus;
import com.nsp.pojo.entity.*;
import com.nsp.pojo.vo.AdminDashboardVO;
import com.nsp.pojo.vo.AdminRecentOrderVO;
import com.nsp.pojo.vo.OrderStatusStatVO;
import com.nsp.pojo.vo.TrendPointVO;
import com.nsp.server.mapper.*;
import com.nsp.server.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘业务实现 — 今日实时统计 + 今日/昨日对比 + 状态分布
 *
 * <p>口径统一：统计卡片、对比图、状态分布均直接查询同一批业务表，保证数据一致；
 * 「今日」= 今日 00:00 至当前时刻。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantApplicationMapper merchantApplicationMapper;
    private final OrderMapper orderMapper;

    @Override
    public AdminDashboardVO getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();

        // 今日/昨日订单
        Long todayOrders = countOrders(todayStart, tomorrowStart);
        Long yesterdayOrders = countOrders(yesterdayStart, todayStart);
        Double todayOrdersChange = calcChange(todayOrders, yesterdayOrders);

        // 今日/昨日营业额
        BigDecimal todayRevenue = sumRevenue(todayStart, tomorrowStart);
        BigDecimal yesterdayRevenue = sumRevenue(yesterdayStart, todayStart);
        Double todayRevenueChange = calcChange(todayRevenue, yesterdayRevenue);

        // 今日/昨日新增用户
        Long todayNewUsers = countNewUsers(todayStart, tomorrowStart);
        Long yesterdayNewUsers = countNewUsers(yesterdayStart, todayStart);
        Double todayNewUsersChange = calcChange(todayNewUsers, yesterdayNewUsers);

        // 待审核商家 / 退款中订单
        Long pendingApplications = merchantApplicationMapper.selectCount(
                new LambdaQueryWrapper<MerchantApplication>()
                        .eq(MerchantApplication::getStatus, "PENDING"));
        Long refundingOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, OrderStatus.REFUNDING));

        return AdminDashboardVO.builder()
                .todayOrders(todayOrders)
                .todayOrdersChange(todayOrdersChange)
                .todayRevenue(todayRevenue)
                .todayRevenueChange(todayRevenueChange)
                .todayNewUsers(todayNewUsers)
                .todayNewUsersChange(todayNewUsersChange)
                .pendingApplications(pendingApplications)
                .refundingOrders(refundingOrders)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<TrendPointVO> getUserTrend(int days) {
        return buildTrend(days, true);
    }

    @Override
    public List<TrendPointVO> getApplicationTrend(int days) {
        return buildTrend(days, false);
    }

    /** 按近 N 日逐日聚合趋势：isUser=true 统计新增用户，否则统计商家入驻申请 */
    private List<TrendPointVO> buildTrend(int days, boolean isUser) {
        int n = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        List<TrendPointVO> result = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = isUser
                    ? countNewUsers(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                    : countApplications(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
            result.add(TrendPointVO.builder()
                    .date(date.format(DATE_FMT))
                    .count(count)
                    .build());
        }
        return result;
    }

    @Override
    public List<OrderStatusStatVO> getOrderStatusDistribution(LocalDate date) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (date != null) {
            wrapper.ge(Order::getCreatedAt, date.atStartOfDay())
                    .lt(Order::getCreatedAt, date.plusDays(1).atStartOfDay());
        }
        List<Order> all = orderMapper.selectList(wrapper);

        Map<String, String> labelMap = Map.of(
                "PENDING_PAY", "待支付",
                "PAID", "已支付",
                "ACCEPTED", "已接单",
                "COOKING", "制作中",
                "COMPLETED", "已完成",
                "CANCELLED", "已取消",
                "REFUNDING", "退款中",
                "REFUNDED", "已退款"
        );

        Map<String, Long> countMap = all.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().getCode(), Collectors.counting()));

        List<String> orderedStatuses = List.of(
                "PENDING_PAY", "PAID", "ACCEPTED", "COOKING",
                "COMPLETED", "CANCELLED", "REFUNDING", "REFUNDED");

        List<OrderStatusStatVO> result = new ArrayList<>();
        for (String status : orderedStatuses) {
            Long count = countMap.getOrDefault(status, 0L);
            if (count > 0) {
                result.add(OrderStatusStatVO.builder()
                        .status(status)
                        .label(labelMap.getOrDefault(status, status))
                        .count(count)
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<AdminRecentOrderVO> getRecentOrders(int limit) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .orderByDesc(Order::getCreatedAt)
                        .last("LIMIT " + Math.min(limit, 50)));

        return orders.stream().map(o -> {
            String username = "";
            User user = userMapper.selectById(o.getUserId());
            if (user != null) username = user.getUsername();

            String merchantName = "";
            Merchant m = merchantMapper.selectById(o.getMerchantId());
            if (m != null) merchantName = m.getName();

            return AdminRecentOrderVO.builder()
                    .orderNo(o.getOrderNo())
                    .username(username)
                    .merchantName(merchantName)
                    .status(o.getStatus().getCode())
                    .totalAmount(o.getTotalAmount())
                    .createdAt(o.getCreatedAt())
                    .build();
        }).toList();
    }

    // ───────────── 内部工具 ─────────────

    /** 统计某时间区间内的订单数 */
    private Long countOrders(LocalDateTime start, LocalDateTime end) {
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, start)
                .lt(Order::getCreatedAt, end));
    }

    /** 统计某时间区间内的营业额（订单实付金额合计） */
    private BigDecimal sumRevenue(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, start)
                .lt(Order::getCreatedAt, end));
        return orders.stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 统计某时间区间内新增用户数 */
    private Long countNewUsers(LocalDateTime start, LocalDateTime end) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreatedAt, start)
                .lt(User::getCreatedAt, end));
    }

    /** 统计某时间区间内商家入驻申请数 */
    private Long countApplications(LocalDateTime start, LocalDateTime end) {
        return merchantApplicationMapper.selectCount(new LambdaQueryWrapper<MerchantApplication>()
                .ge(MerchantApplication::getCreatedAt, start)
                .lt(MerchantApplication::getCreatedAt, end));
    }

    /** 计算较昨日变化百分比；昨日为 0 时返回 null（前端显示「无对比」） */
    private Double calcChange(Long today, Long yesterday) {
        if (yesterday == null || yesterday == 0) return null;
        return (today - yesterday) * 100.0 / yesterday;
    }

    /** 计算较昨日变化百分比（金额版）；昨日为 0 时返回 null */
    private Double calcChange(BigDecimal today, BigDecimal yesterday) {
        if (yesterday == null || yesterday.compareTo(BigDecimal.ZERO) == 0) return null;
        return today.subtract(yesterday)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterday, 2, java.math.RoundingMode.HALF_UP)
                .doubleValue();
    }

}
