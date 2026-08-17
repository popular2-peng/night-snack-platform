package com.nsp.server.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.pojo.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 商家在售商品列表缓存 — Cache Aside（旁路缓存）
 *
 * <p>用户端首页商家卡片商品预览（在售商品列表）读多写少：
 * 首次查询回源 MySQL 并写入缓存，后续直接命中；商品增删改/上下架/规格变更时调用 {@link #evict(Long)} 清除，
 * 下次请求再回源写入。
 */
@Component
@RequiredArgsConstructor
public class MerchantProductsCache {

    private static final Logger log = LoggerFactory.getLogger(MerchantProductsCache.class);

    private static final String KEY_PREFIX = "merchant:products:on:";
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private String key(Long merchantId) {
        return KEY_PREFIX + merchantId;
    }

    /** 读取缓存；未命中或反序列化失败返回 null（调用方回源 MySQL） */
    public List<ProductVO> get(Long merchantId) {
        String json = redisTemplate.opsForValue().get(key(merchantId));
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ProductVO>>() {});
        } catch (Exception e) {
            log.warn("商家在售商品缓存反序列化失败，回源 MySQL: merchantId={}, err={}", merchantId, e.getMessage());
            return null;
        }
    }

    /** 写入缓存 */
    public void put(Long merchantId, List<ProductVO> list) {
        try {
            redisTemplate.opsForValue().set(key(merchantId), objectMapper.writeValueAsString(list), TTL);
            log.info("商家在售商品缓存已写入: merchantId={}, 商品数={}", merchantId, list.size());
        } catch (Exception e) {
            log.warn("商家在售商品缓存写入失败: merchantId={}, err={}", merchantId, e.getMessage());
        }
    }

    /** 清除缓存（商品增删改 / 上下架 / 规格变更时调用） */
    public void evict(Long merchantId) {
        redisTemplate.delete(key(merchantId));
        log.info("商家在售商品缓存已清除: merchantId={}", merchantId);
    }
}
