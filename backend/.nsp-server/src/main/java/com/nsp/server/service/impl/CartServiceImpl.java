package com.nsp.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.CartItemDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.Product;
import com.nsp.pojo.entity.ProductSpec;
import com.nsp.pojo.vo.CartGroupVO;
import com.nsp.pojo.vo.CartItemVO;
import com.nsp.pojo.vo.CartVO;
import com.nsp.server.mapper.MerchantMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 购物车业务实现 — Redis Hash
 *
 * <p>Key: cart:{userId} | Field: 购物车条目键 | Value: quantity
 * <p>条目键规则：有规格商品 = `{specId}`；无规格商品 = `p{productId}`
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
    /** 无规格商品条目键前缀：p{productId} */
    private static final String PLAIN_PRODUCT_PREFIX = "p";

    private final StringRedisTemplate redisTemplate;
    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final MerchantMapper merchantMapper;

    @Override
    public void addItem(Long userId, CartItemDTO dto, Long merchantId) {
        String cartKey = cartKey(userId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();

        // 跨店切换：清空原店铺商品（购物车只允许一个商家的商品）
        String existingMerchant = ops.get(cartKey, MERCHANT_HASH_KEY);
        if (existingMerchant != null && !existingMerchant.equals(String.valueOf(merchantId))) {
            redisTemplate.delete(cartKey);
            log.info("跨店切换，清空购物车: userId={}, old={}, new={}", userId, existingMerchant, merchantId);
        }

        // 1. 先校验商品（必填）：存在、属于当前商家、上架中
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null || !product.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCode.PRODUCT_NOT_FOUND,
                    String.format(MessageConst.PRODUCT_NOT_BELONG_MERCHANT_MSG, dto.getProductId()));
        }
        if (!"ON".equals(product.getStatus())) {
            throw new BizException(ErrorCode.PRODUCT_OFF_SHELF,
                    String.format(MessageConst.PRODUCT_OFF_SHELF_MSG, product.getName()));
        }

        // 2. 再判断规格（选填）：传了 specId 走规格（加价/扣库存维度），没传按无规格商品（商品基础价）
        String fieldKey;
        BigDecimal unitPrice;
        if (dto.getSpecId() != null) {
            ProductSpec spec = productSpecMapper.selectById(dto.getSpecId());
            if (spec == null || !spec.getProductId().equals(product.getId())) {
                throw new BizException(ErrorCode.SPEC_NOT_FOUND,
                        String.format(MessageConst.SPEC_NOT_FOUND_WITH_ID_MSG, dto.getSpecId()));
            }
            fieldKey = String.valueOf(spec.getId());
            unitPrice = product.getPrice().add(spec.getPriceDelta());
        } else {
            // 无规格商品：校验该商品确实未配置规格（有规格必须选规格，不能按基础价绕过）
            Long specCount = productSpecMapper.selectCount(
                    new LambdaQueryWrapper<ProductSpec>().eq(ProductSpec::getProductId, product.getId()));
            if (specCount > 0) {
                throw new BizException(ErrorCode.PRODUCT_HAS_SPECS,
                        String.format(MessageConst.PRODUCT_HAS_SPECS_MSG, product.getName()));
            }
            fieldKey = PLAIN_PRODUCT_PREFIX + product.getId();
            unitPrice = product.getPrice();
        }

        // 3. 数量累加（同一条目合并），上限 99
        String currentQty = ops.get(cartKey, fieldKey);
        int newQty = (currentQty == null ? 0 : Integer.parseInt(currentQty)) + dto.getQuantity();
        if (newQty > MAX_QUANTITY) {
            throw new BizException(ErrorCode.CART_ITEM_LIMIT, String.format(MessageConst.CART_ITEM_LIMIT_MSG, MAX_QUANTITY));
        }

        ops.put(cartKey, fieldKey, String.valueOf(newQty));
        ops.put(cartKey, MERCHANT_HASH_KEY, String.valueOf(merchantId));
        redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);

        log.info("加入购物车: userId={}, productId={}, specId={}, fieldKey={}, qty={}",
                userId, product.getId(), dto.getSpecId(), fieldKey, newQty);
    }

    @Override
    public void removeItem(Long userId, String cartKeyField) {
        redisTemplate.opsForHash().delete(cartKey(userId), cartKeyField);
        log.info("移除购物车: userId={}, fieldKey={}", userId, cartKeyField);
    }

    @Override
    public void updateQuantity(Long userId, String cartKeyField, Integer quantity) {
        // 防御：更新接口必须携带条目键（有规格={specId}，无规格=p{productId}）
        if (cartKeyField == null || cartKeyField.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "更新购物车数量：cartKey 不能为空");
        }
        if (quantity <= 0) {
            removeItem(userId, cartKeyField);
            return;
        }
        if (quantity > MAX_QUANTITY) {
            throw new BizException(ErrorCode.CART_ITEM_LIMIT, String.format(MessageConst.CART_ITEM_LIMIT_MSG, MAX_QUANTITY));
        }
        String cartKey = cartKey(userId);
        redisTemplate.opsForHash().put(cartKey, cartKeyField, String.valueOf(quantity));
        redisTemplate.expire(cartKey, CART_TTL_DAYS, TimeUnit.DAYS);
        log.info("更新购物车数量: userId={}, fieldKey={}, qty={}", userId, cartKeyField, quantity);
    }

    @Override
    public CartVO getCart(Long userId) {
        String cartKey = cartKey(userId);
        HashOperations<String, String, String> ops = redisTemplate.opsForHash();
        Map<String, String> entries = ops.entries(cartKey);

        CartVO cartVO = new CartVO();
        cartVO.setGroups(new ArrayList<>());
        cartVO.setTotalPrice(BigDecimal.ZERO);

        if (entries.isEmpty()) {
            return cartVO;
        }

        // 按商家分组：条目通过商品推导所属商家（key = merchantId → 组）
        Map<Long, CartGroupVO> groupMap = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (MERCHANT_HASH_KEY.equals(entry.getKey())) {
                continue; // 商家标记键，不参与条目组装（商家由商品推导）
            }

            String fieldKey = entry.getKey();
            int qty = Integer.parseInt(entry.getValue());

            // 解析条目 → (商品, 规格, 单价)
            Product product;
            ProductSpec spec = null;
            BigDecimal unitPrice;
            if (fieldKey.startsWith(PLAIN_PRODUCT_PREFIX)) {
                // 无规格商品：条目键 `p{productId}`，按商品基础价
                Long productId = Long.valueOf(fieldKey.substring(1));
                product = productMapper.selectById(productId);
                if (product == null) continue;
                unitPrice = product.getPrice();
            } else {
                // 有规格商品：条目键 `{specId}`
                Long specId = Long.valueOf(fieldKey);
                spec = productSpecMapper.selectById(specId);
                if (spec == null) continue;
                product = productMapper.selectById(spec.getProductId());
                if (product == null) continue;
                unitPrice = product.getPrice().add(spec.getPriceDelta());
            }

            // 取（或创建）该商家的分组
            CartGroupVO group = groupMap.computeIfAbsent(product.getMerchantId(), mid -> {
                CartGroupVO g = new CartGroupVO();
                g.setMerchantId(mid);
                Merchant merchant = merchantMapper.selectById(mid);
                g.setMerchantName(merchant != null ? merchant.getName() : "");
                g.setItems(new ArrayList<>());
                g.setTotalPrice(BigDecimal.ZERO);
                return g;
            });

            // 组装条目
            CartItemVO item = new CartItemVO();
            item.setCartKey(fieldKey);
            item.setSpecId(spec != null ? spec.getId() : null);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSpecName(spec != null ? spec.getName() : null);
            item.setPrice(unitPrice);
            item.setQuantity(qty);
            item.setImageUrl(product.getImageUrl());
            group.getItems().add(item);

            // 组小计 + 全局合计
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
            group.setTotalPrice(group.getTotalPrice().add(itemTotal));
            cartVO.setTotalPrice(cartVO.getTotalPrice().add(itemTotal));
        }

        cartVO.setGroups(new ArrayList<>(groupMap.values()));
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
