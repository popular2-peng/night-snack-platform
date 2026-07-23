package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 订单状态分布 — 供饼图/柱状图
 */
@Data
@Builder
public class OrderStatusStatVO {

    /** 状态码（如 PENDING_PAY） */
    private String status;

    /** 状态中文名（如 待支付） */
    private String label;

    /** 该状态下的订单数量 */
    private Long count;

}
