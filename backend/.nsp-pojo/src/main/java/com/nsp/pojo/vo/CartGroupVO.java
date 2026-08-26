package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车商家分组 — 一个商家的商品集合
 */
@Data
public class CartGroupVO {

    /** 商家 ID */
    private Long merchantId;

    /** 商家名称 */
    private String merchantName;

    /** 该商家商品列表 */
    private List<CartItemVO> items;

    /** 该商家商品总金额 */
    private BigDecimal totalPrice;

}
