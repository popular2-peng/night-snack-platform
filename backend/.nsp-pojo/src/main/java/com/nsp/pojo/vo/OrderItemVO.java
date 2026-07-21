package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细项响应
 */
@Data
public class OrderItemVO {

    private Long productId;
    private Long specId;
    /** 商品名称 + 规格名称快照 */
    private String nameSnapshot;
    /** 下单时单价 */
    private BigDecimal priceSnapshot;
    private Integer quantity;
    /** 商品图片 URL */
    private String imageUrl;

}
