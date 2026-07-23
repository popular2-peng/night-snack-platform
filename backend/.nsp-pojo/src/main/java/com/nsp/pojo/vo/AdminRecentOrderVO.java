package com.nsp.pojo.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台最新订单一览
 */
@Data
@Builder
public class AdminRecentOrderVO {

    /** 订单号 */
    private String orderNo;

    /** 下单用户名 */
    private String username;

    /** 商家名 */
    private String merchantName;

    /** 订单状态 */
    private String status;

    /** 实付金额 */
    private BigDecimal totalAmount;

    /** 下单时间 */
    private LocalDateTime createdAt;

}
