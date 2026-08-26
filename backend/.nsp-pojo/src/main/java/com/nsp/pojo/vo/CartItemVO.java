package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车中的单个商品
 */
@Data
public class CartItemVO {

    /** 购物车条目键（更新/删除接口用）：有规格=`{specId}`，无规格=`p{productId}` */
    private String cartKey;

    /** 规格 ID（无规格商品为 null） */
    private Long specId;

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 规格名称 */
    private String specName;

    /** 单价（基础价 + 规格加价） */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 商品图片 URL */
    private String imageUrl;

}
