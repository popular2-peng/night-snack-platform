package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 商家工作台 — 首页经营概况
 */
@Data
@Builder
public class MerchantDashboardVO {

    /** 店铺名称 */
    private String shopName;

    /** 营业状态：OPEN / CLOSED / PENDING / SUSPENDED */
    private String shopStatus;

    /** 今日订单数（今日下单订单数） */
    private Long todayOrders;

    /** 今日营业额（今日下单订单实付金额合计，排除已取消/已退款） */
    private BigDecimal todayRevenue;

    /** 待接单数（当前 status=PAID 的订单数，全量） */
    private Long pendingAccept;

    /** 在售商品数（status=ON） */
    private Long onSaleProducts;

    /** 今日订单状态分布（status → 数量） */
    private Map<String, Long> orderStatusStats;

    /** 统计时间 */
    private LocalDateTime updatedAt;

}
