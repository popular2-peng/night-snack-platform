package com.nsp.common.enum;

import lombok.Getter;

/**
 * 订单状态 — 状态机流转规则见 {@code OrderService} 实现
 *
 * <pre>
 * PENDING_PAY → PAID → ACCEPTED → COOKING → COMPLETED
 * PENDING_PAY → CANCELLED（超时未支付 / 用户取消）
 * PAID → CANCELLED（商家拒单，触发退款）
 * COMPLETED → REFUNDING → REFUNDED（售后退款）
 * </pre>
 */
@Getter
public enum OrderStatus {

    /** 待支付 */
    PENDING_PAY("PENDING_PAY", "待支付"),

    /** 已支付 */
    PAID("PAID", "已支付"),

    /** 商家已接单 */
    ACCEPTED("ACCEPTED", "已接单"),

    /** 制作中 */
    COOKING("COOKING", "制作中"),

    /** 已完成 */
    COMPLETED("COMPLETED", "已完成"),

    /** 已取消 */
    CANCELLED("CANCELLED", "已取消"),

    /** 退款中 */
    REFUNDING("REFUNDING", "退款中"),

    /** 已退款 */
    REFUNDED("REFUNDED", "已退款");

    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus fromCode(String code) {
        for (OrderStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) return s;
        }
        return null;
    }

}
