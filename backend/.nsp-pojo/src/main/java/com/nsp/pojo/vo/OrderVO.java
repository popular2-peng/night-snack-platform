package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情响应
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long merchantId;
    private String merchantName;
    private String status;
    private BigDecimal totalAmount;
    private String addressSnapshot;
    private String payChannel;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime payTime;

    /** 订单明细 */
    private List<OrderItemVO> items;

    /** 状态流转日志 */
    private List<OrderStatusLogVO> statusLogs;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

}
