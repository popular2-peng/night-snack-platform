package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 管理后台核心统计 — 平台运营视角（非商家视角）
 */
@Data
@Builder
public class AdminDashboardVO {

    /** 平台总用户数（顾客） */
    private Long totalUsers;

    /** 商家总数 */
    private Long totalMerchants;

    /** 平台总订单数（历史累计） */
    private Long totalOrders;

    /** 待审核的商家入驻申请数 */
    private Long pendingApplications;

}
