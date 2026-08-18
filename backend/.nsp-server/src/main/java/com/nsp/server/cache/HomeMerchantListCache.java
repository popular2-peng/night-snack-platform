package com.nsp.server.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.pojo.vo.MerchantVO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 首页商家列表缓存 — Cache Aside（旁路缓存）
 *
 * <p>用户端首页商家列表（全部营业中商家）读多写少：首次查询回源 MySQL 并写入缓存，
 * 后续直接命中缓存，降低 MySQL 压力；商家状态变更 / 新商家入驻时调用 {@link #evict()} 清除缓存，
 * 下次请求再回源并重新写入。
 */
@Component
@RequiredArgsConstructor
public class HomeMerchantListCache {

    private static final Logger log = LoggerFactory.getLogger(HomeMerchantListCache.class);

    /** 缓存 Key（全量营业中商家列表；v2：MerchantVO 增加 categoryId 字段后结构变化，版本号强制旧缓存失效） */
    private static final String KEY = "merchant:list:home:v2";

    /** TTL：30 分钟（兜底过期，正常依赖主动清除） */
    private static final Duration TTL = Duration.ofMinutes(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 读取缓存；未命中或反序列化失败返回 null（调用方回源 MySQL） */
    public List<MerchantVO> get() {
        String json = redisTemplate.opsForValue().get(KEY);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MerchantVO>>() {});
        } catch (Exception e) {
            log.warn("首页商家列表缓存反序列化失败，回源 MySQL: {}", e.getMessage());
            return null;
        }
    }

    /** 写入缓存 */
    public void put(List<MerchantVO> list) {
        try {
            redisTemplate.opsForValue().set(KEY, objectMapper.writeValueAsString(list), TTL);
            log.info("首页商家列表缓存已写入，商家数={}", list.size());
        } catch (Exception e) {
            log.warn("首页商家列表缓存写入失败: {}", e.getMessage());
        }
    }

    /** 清除缓存（商家状态变更 / 新商家入驻时调用） */
    public void evict() {
        redisTemplate.delete(KEY);
        log.info("首页商家列表缓存已清除");
    }
}
