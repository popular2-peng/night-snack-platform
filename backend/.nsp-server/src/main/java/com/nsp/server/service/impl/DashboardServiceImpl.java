package com.nsp.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.pojo.entity.*;
import com.nsp.pojo.vo.AdminDashboardVO;
import com.nsp.pojo.vo.AdminRecentOrderVO;
import com.nsp.pojo.vo.OrderStatusStatVO;
import com.nsp.server.mapper.*;
import com.nsp.server.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘业务实现
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantApplicationMapper merchantApplicationMapper;
    private final OrderMapper orderMapper;

    @Override
    public AdminDashboardVO getStats() {
        Long totalUsers = userMapper.selectCount(null);
        Long totalMerchants = merchantMapper.selectCount(null);
        Long totalOrders = orderMapper.selectCount(null);
        Long pendingApplications = merchantApplicationMapper.selectCount(
                new LambdaQueryWrapper<MerchantApplication>()
                        .eq(MerchantApplication::getStatus, "PENDING"));

        return AdminDashboardVO.builder()
                .totalUsers(totalUsers)
                .totalMerchants(totalMerchants)
                .totalOrders(totalOrders)
                .pendingApplications(pendingApplications)
                .build();
    }

    @Override
    public List<OrderStatusStatVO> getOrderStatusDistribution() {
        List<Order> all = orderMapper.selectList(null);

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

}
