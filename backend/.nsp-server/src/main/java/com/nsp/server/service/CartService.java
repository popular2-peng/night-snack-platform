package com.nsp.server.service;

import com.nsp.pojo.dto.CartItemDTO;
import com.nsp.pojo.vo.CartVO;

/**
 * 购物车业务接口 — 基于 Redis Hash 存储
 *
 * <p>Redis Key 模式: {@code cart:{userId}}，Hash 字段为条目键，值为 quantity
 * <p>条目键规则：有规格商品 = `{specId}`；无规格商品 = `p{productId}`
 * <p>业务规则：
 * <ul>
 *   <li>未登录不能加购</li>
 *   <li>同一时间购物车内只能有一个商家的商品，切换商家时清空</li>
 *   <li>单个商品数量上限 99</li>
 * </ul>
 */
public interface CartService {

    /**
     * 添加商品到购物车（如已存在则更新数量）
     * <p>productId 必填、specId 选填：有规格商品传 specId（按规格加价/扣库存），无规格商品只传 productId（按商品基础价）
     */
    void addItem(Long userId, CartItemDTO dto, Long merchantId);

    /**
     * 移除购物车中的商品（条目键：有规格=`{specId}`，无规格=`p{productId}`）
     */
    void removeItem(Long userId, String cartKeyField);

    /**
     * 更新商品数量（条目键：有规格=`{specId}`，无规格=`p{productId}`）
     */
    void updateQuantity(Long userId, String cartKeyField, Integer quantity);

    /**
     * 查询购物车
     */
    CartVO getCart(Long userId);

    /**
     * 清空购物车
     */
    void clearCart(Long userId);

}
