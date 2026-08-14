package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表项响应 — 含商品明细与地址快照，商家端订单卡片可直接渲染
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

    /** 商品明细（名称/单价/数量快照） */
    private List<OrderItemVO> items;

    /** 收货地址快照 JSON（receiver / phone / detail） */
    private String addressSnapshot;

}
