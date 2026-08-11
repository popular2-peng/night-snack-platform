package com.nsp.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.pojo.entity.Order;
import com.nsp.pojo.entity.OrderStatusLog;
import com.nsp.server.mapper.OrderMapper;
import com.nsp.server.mapper.OrderStatusLogMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动任务（每 60 秒扫描一次）
 *
 * <pre>
 * 1. PENDING_PAY 下单后 15 分钟未支付        → 自动取消（释放库存）
 * 2. PAID        支付后 15 分钟商家未接单     → 自动取消（释放库存 + mock 退款）
 * 3. COOKING     制作中超过 30 分钟           → 自动完成（视为已送达）
 * </pre>
 *
 * <p>状态停留时长的判定依据：该状态下最后一条 {@code order_status_log.created_at}，
 * 避免依赖 `updated_at`（插入种子数据时 updated_at 为建库时刻，不代表状态变更时刻）。
 */
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    /** 未支付自动取消时限（分钟） */
    private static final int UNPAID_CANCEL_MINUTES = 15;
    /** 未接单自动取消时限（分钟） */
    private static final int UNACCEPTED_CANCEL_MINUTES = 15;
    /** 制作中自动完成时限（分钟） */
    private static final int COOKING_COMPLETE_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 60_000)
    public void scan() {
        try {
            cancelUnpaidOrders();
            cancelUnacceptedOrders();
            autoCompleteCookingOrders();
        } catch (Exception e) {
            // 定时任务兜底：单次扫描异常不中断后续轮询
            log.error("订单超时扫描异常", e);
        }
    }

    /** PENDING_PAY 下单后 15 分钟未支付 → 自动取消 */
    private void cancelUnpaidOrders() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING_PAY"));
        for (Order o : orders) {
            LocalDateTime lastLog = lastStatusTime(o.getId(), "PENDING_PAY");
            if (lastLog != null && lastLog.isBefore(LocalDateTime.now().minusMinutes(UNPAID_CANCEL_MINUTES))) {
                orderService.timeoutCancel(o.getId(), "超时未支付自动取消");
            }
        }
    }

    /** PAID 支付后 15 分钟商家未接单 → 自动取消（已支付则退款） */
    private void cancelUnacceptedOrders() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PAID"));
        for (Order o : orders) {
            LocalDateTime lastLog = lastStatusTime(o.getId(), "PAID");
            if (lastLog != null && lastLog.isBefore(LocalDateTime.now().minusMinutes(UNACCEPTED_CANCEL_MINUTES))) {
                orderService.timeoutCancel(o.getId(), "超时未接单自动取消(已退款)");
            }
        }
    }

    /** COOKING 制作中超过 30 分钟 → 自动完成（商家自配送，超时视为已送达） */
    private void autoCompleteCookingOrders() {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "COOKING"));
        for (Order o : orders) {
            LocalDateTime lastLog = lastStatusTime(o.getId(), "COOKING");
            if (lastLog != null && lastLog.isBefore(LocalDateTime.now().minusMinutes(COOKING_COMPLETE_MINUTES))) {
                orderService.timeoutComplete(o.getId());
            }
        }
    }

    /** 查询某订单进入指定状态的最后一条日志时间（该状态下的停留起点） */
    private LocalDateTime lastStatusTime(Long orderId, String toStatus) {
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .eq(OrderStatusLog::getToStatus, toStatus)
                        .orderByDesc(OrderStatusLog::getCreatedAt)
                        .last("LIMIT 1"));
        return logs.isEmpty() ? null : logs.get(0).getCreatedAt();
    }

}
