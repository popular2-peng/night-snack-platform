package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台首页统计
 */
@Data
@Builder
public class AdminDashboardVO {

    /** 平台总用户数 */
    private Long totalUsers;

    /** 商家总数 */
    private Long totalMerchants;

    /** 今日订单数 */
    private Long todayOrders;

    /** 今日营业额 */
    private BigDecimal todayRevenue;

}
