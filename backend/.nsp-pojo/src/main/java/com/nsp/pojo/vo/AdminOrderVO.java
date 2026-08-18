package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台订单列表项 — 在 {@link OrderListVO} 基础上补充下单用户信息
 */
@Data
public class AdminOrderVO {

    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 下单用户名（user 表，后端生成的展示名） */
    private String username;

    /** 商家名 */
    private String merchantName;

    /** 订单状态码（如 PENDING_PAY） */
    private String status;

    /** 实付总金额 */
    private BigDecimal totalAmount;

    /** 支付渠道（alipay / wechat_mock） */
    private String payChannel;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

}
