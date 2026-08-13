package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录 — 记录每次支付请求和回调
 */
@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 支付渠道：ALIPAY / WECHAT_MOCK */
    private String channel;

    /** 第三方交易号（支付宝 trade_no / 微信 transaction_id） */
    private String tradeNo;

    /** 退款请求号（商户侧生成，重试时复用，保证渠道侧幂等） */
    private String outRefundNo;

    /** 支付金额 */
    private BigDecimal amount;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 支付状态：PENDING / SUCCESS / FAILED / REFUNDED */
    private String status;

    /** 退款状态：REFUNDING（退款中，失败可重试）/ REFUNDED（已退款） */
    private String refundStatus;

    /** 回调通知次数 */
    private Integer notifyCount;

    /** 最后一次回调时间 */
    private LocalDateTime lastNotifyAt;

    /** 退款完成时间 */
    private LocalDateTime refundAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
