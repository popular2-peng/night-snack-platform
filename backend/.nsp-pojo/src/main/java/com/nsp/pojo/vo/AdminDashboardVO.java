package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台首页仪表盘 — 今日核心指标 + 较昨日对比
 *
 * <p>口径统一：「今日」均为今日 00:00 至当前时刻的实时聚合，
 * 与趋势图、状态分布来自同一数据源，保证页面各模块数据一致。
 */
@Data
@Builder
public class AdminDashboardVO {

    /** 今日订单数 */
    private Long todayOrders;

    /** 今日订单较昨日变化百分比（昨日为 0 时为 null） */
    private Double todayOrdersChange;

    /** 今日营业额（元） */
    private BigDecimal todayRevenue;

    /** 今日营业额较昨日变化百分比 */
    private Double todayRevenueChange;

    /** 今日新增用户数 */
    private Long todayNewUsers;

    /** 今日新增用户较昨日变化百分比 */
    private Double todayNewUsersChange;

    /** 待审核的商家入驻申请数 */
    private Long pendingApplications;

    /** 退款中订单数 */
    private Long refundingOrders;

    /** 数据更新时间 */
    private LocalDateTime updatedAt;

}
