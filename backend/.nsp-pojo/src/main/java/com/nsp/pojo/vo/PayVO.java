package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付响应
 */
@Data
@Builder
public class PayVO {

    private String orderNo;
    private String channel;
    private BigDecimal amount;
    private String status;
    /** 支付宝返回的支付二维码链接 */
    private String qrCode;

}
