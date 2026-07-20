package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车响应
 */
@Data
public class CartVO {

    /** 商家 ID（购物车只能有一个商家的商品） */
    private Long merchantId;

    /** 商家名称 */
    private String merchantName;

    /** 购物车商品列表 */
    private List<CartItemVO> items;

    /** 总金额 */
    private BigDecimal totalPrice;

}
