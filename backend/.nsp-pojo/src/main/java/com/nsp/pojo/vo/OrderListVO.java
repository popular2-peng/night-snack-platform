package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项响应（简版，不含明细和日志）
 */
@Data
public class OrderListVO {

    private Long id;
    private String orderNo;
    private String merchantName;
    private String status;
    private BigDecimal totalAmount;
    private String payChannel;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

}
