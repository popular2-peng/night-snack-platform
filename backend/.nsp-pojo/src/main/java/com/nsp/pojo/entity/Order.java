package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nsp.common.enum.OrderStatus;
import com.nsp.common.enum.PayChannel;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 — 一条订单包含一个商家的多个商品
 */
@Data
@TableName("order")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（业务唯一，如 "NSP202507211530001"） */
    private String orderNo;

    /** 下单用户 ID */
    private Long userId;

    /** 商家 ID */
    private Long merchantId;

    /** 订单状态 */
    private OrderStatus status;

    /** 实付总金额 */
    private BigDecimal totalAmount;

    /** 收货地址快照（JSON 字符串，下单时锁定） */
    private String addressSnapshot;

    /** 支付渠道 */
    private PayChannel payChannel;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
