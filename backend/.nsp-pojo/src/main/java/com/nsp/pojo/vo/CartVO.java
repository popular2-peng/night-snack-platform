package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 购物车响应 — 按商家分组
 *
 * <p>当前业务购物车只允许一个商家的商品（跨店加购自动清空），但接口结构按商家分组返回，
 * 前端按当前商家取组展示（浏览其他商家时不显示购物车），也为后续支持多商家购物车预留结构。
 */
@Data
public class CartVO {

    /** 按商家分组的购物车条目（每组 = 一个商家的商品） */
    private List<CartGroupVO> groups;

    /** 全部商家商品总金额 */
    private BigDecimal totalPrice;

}
