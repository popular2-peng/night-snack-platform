package com.nsp.server.service;

import com.nsp.pojo.dto.CartItemDTO;
import com.nsp.pojo.vo.CartVO;

/**
 * 购物车业务接口 — 基于 Redis Hash 存储
 *
 * <p>Redis Key 模式: {@code cart:{userId}}，Hash 字段为 specId，值为 quantity
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
     */
    void addItem(Long userId, CartItemDTO dto, Long merchantId);

    /**
     * 移除购物车中的商品
     */
    void removeItem(Long userId, Long specId);

    /**
     * 更新商品数量
     */
    void updateQuantity(Long userId, Long specId, Integer quantity);

    /**
     * 查询购物车
     */
    CartVO getCart(Long userId);

    /**
     * 清空购物车
     */
    void clearCart(Long userId);

}
