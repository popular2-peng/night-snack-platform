package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品规格响应
 */
@Data
public class SpecVO {

    private Long id;

    /** 规格名称（如"大杯""加珍珠"） */
    private String name;

    /** 加价 */
    private BigDecimal priceDelta;

    /** 库存数量（用于前端展示是否可售） */
    private Integer stock;

}
