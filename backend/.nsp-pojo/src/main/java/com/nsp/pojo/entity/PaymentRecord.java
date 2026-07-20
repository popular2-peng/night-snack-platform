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

    /** 支付金额 */
    private BigDecimal amount;

    /** 支付状态：PENDING / SUCCESS / FAILED / REFUNDED */
    private String status;

    /** 回调通知次数 */
    private Integer notifyCount;

    /** 最后一次回调时间 */
    private LocalDateTime lastNotifyAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
