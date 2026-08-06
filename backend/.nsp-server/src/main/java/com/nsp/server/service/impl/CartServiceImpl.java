package com.nsp.server.service.impl;

import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.CartItemDTO;
import com.nsp.pojo.entity.Product;
import com.nsp.pojo.entity.ProductSpec;
import com.nsp.pojo.vo.CartItemVO;
import com.nsp.pojo.vo.CartVO;
import com.nsp.server.mapper.ProductMapper;
import com.nsp.server.mapper.ProductSpecMapper;
import com.nsp.server.service.CartService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 购物车业务实现 — Redis Hash
 *
 * <p>Key: cart:{userId} | Field: specId | Value: quantity
 * <p>TTL: 7 天
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);
    private static final String CART_KEY_PREFIX = "cart:";
    private static final int MAX_QUANTITY = 99;
    private static final int CART_TTL_DAYS = 7;
    private static final String MERCHANT_HASH_KEY = "__merchant_id__";

    private final StringRedisTemplate redisTemplate;
    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;

    @Override
    public void addItem(Long userId, CartItemDTO dto, Long merchantId) {
        String cartKey = cartKey(userId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();

        String existingMerchant = ops.get(cartKey, MERCHANT_HASH_KEY);
        if (existingMerchant != null && !existingMerchant.equals(String.valueOf(merchantId))) {
            redisTemplate.delete(cartKey);
            log.info("跨店切换，清空购物车: userId={}, old={}, new={}", userId, existingMerchant, merchantId);
        }

        ProductSpec spec = productSpecMapper.selectById(dto.getSpecId());
        if (spec == null) {
            throw new BizException(ErrorCode.SPEC_NOT_FOUND, MessageConst.SPEC_NOT_FOUND_MSG);
        }
        Product product = productMapper.selectById(spec.getProductId());
        if (product == null || !product.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND, MessageConst.CART_PRODUCT_NOT_BELONG_MSG);
        }

        String currentQty = ops.get(cartKey, String.valueOf(dto.getSpecId()));
        int newQty = (currentQty == null ? 0 : Integer.parseInt(currentQty)) + dto.getQuantity();
        if (newQty > MAX_QUANTITY) {
            throw new BizException(ErrorCode.CART_ITEM_LIMIT, String.format(MessageConst.CART_ITEM_LIMIT_MSG, MAX_QUANTITY));
        }

        ops.put(cartKey, String.valueOf(dto.getSpecId()), String.valueOf(newQty));
        ops.put(cartKey, MERCHANT_HASH_KEY, String.valueOf(merchantId));
        redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);

        log.info("加入购物车: userId={}, specId={}, qty={}", userId, dto.getSpecId(), newQty);
    }

    @Override
    public void removeItem(Long userId, Long specId) {
        redisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(specId));
        log.info("移除购物车: userId={}, specId={}", userId, specId);
    }

    @Override
    public void updateQuantity(Long userId, Long specId, Integer quantity) {
        if (quantity <= 0) {
            removeItem(userId, specId);
            return;
        }
        if (quantity > MAX_QUANTITY) {
            throw new BizException(ErrorCode.CART_ITEM_LIMIT, String.format(MessageConst.CART_ITEM_LIMIT_MSG, MAX_QUANTITY));
        }
        String cartKey = cartKey(userId);
        redisTemplate.opsForHash().put(cartKey, String.valueOf(specId), String.valueOf(quantity));
        redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);
        log.info("更新购物车数量: userId={}, specId={}, qty={}", userId, specId, quantity);
    }

    @Override
    public CartVO getCart(Long userId) {
        String cartKey = cartKey(userId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> entries = ops.entries(cartKey);

        CartVO cartVO = new CartVO();
        cartVO.setItems(new ArrayList<>());
        cartVO.setTotalPrice(BigDecimal.ZERO);

        if (entries.isEmpty()) {
            return cartVO;
        }

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (MERCHANT_HASH_KEY.equals(entry.getKey())) {
                cartVO.setMerchantId(Long.valueOf(entry.getValue()));
                continue;
            }

            Long specId = Long.valueOf(entry.getKey());
            int qty = Integer.parseInt(entry.getValue());

            ProductSpec spec = productSpecMapper.selectById(specId);
            if (spec == null) continue;

            Product product = productMapper.selectById(spec.getProductId());
            if (product == null) continue;

            BigDecimal unitPrice = product.getPrice().add(spec.getPriceDelta());
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            cartVO.setTotalPrice(cartVO.getTotalPrice().add(itemTotal));

            CartItemVO item = new CartItemVO();
            item.setSpecId(specId);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSpecName(spec.getName());
            item.setPrice(unitPrice);
            item.setQuantity(qty);
            item.setImageUrl(product.getImageUrl());
            cartVO.getItems().add(item);
        }

        return cartVO;
    }

    @Override
    public void clearCart(Long userId) {
        redisTemplate.delete(cartKey(userId));
        log.info("清空购物车: userId={}", userId);
    }

    private String cartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

}
