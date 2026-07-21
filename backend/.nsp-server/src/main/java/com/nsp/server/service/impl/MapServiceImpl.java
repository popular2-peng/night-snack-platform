package com.nsp.server.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.common.util.HaversineUtil;
import com.nsp.server.service.MapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 地图服务实现 — 百度地图 Web API 优先 + Haversine 降级 + Redis 距离缓存
 *
 * <p>缓存 Key: shop:distance:cache:{shopId}_{userLat}_{userLng}
 * <p>TTL: 1 小时
 */
@Service
public class MapServiceImpl implements MapService {

    private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);
    private static final String CACHE_KEY_PREFIX = "shop:distance:cache:";
    private static final int CACHE_TTL_HOURS = 1;

    @Value("${baidu.map.ak}")
    private String baiduAk;

    @Value("${baidu.map.distance-api}")
    private String distanceApi;

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MapServiceImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    @Override
    public long calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                  BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return 0;
        }
        return calculateWithFallback(lat1, lng1, lat2, lng2);
    }

    /**
     * 计算距离并缓存
     *
     * @param shopLat  商家纬度
     * @param shopLng  商家经度
     * @param userLat  用户纬度
     * @param userLng  用户经度
     * @param shopId   商家 ID（用于缓存 Key）
     * @return 距离（米）
     */
    public long calculateDistanceWithCache(BigDecimal shopLat, BigDecimal shopLng,
                                           BigDecimal userLat, BigDecimal userLng,
                                           Long shopId) {
        if (shopLat == null || shopLng == null || userLat == null || userLng == null) {
            return 0;
        }

        // 尝试从 Redis 缓存读取
        String cacheKey = CACHE_KEY_PREFIX + shopId + "_"
                + userLat.setScale(4, RoundingMode.HALF_UP) + "_"
                + userLng.setScale(4, RoundingMode.HALF_UP);
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("距离缓存命中: key={}, dist={}", cacheKey, cached);
            return Long.parseLong(cached);
        }

        long dist = calculateWithFallback(shopLat, shopLng, userLat, userLng);

        // 写入缓存
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(dist),
                CACHE_TTL_HOURS, TimeUnit.HOURS);

        return dist;
    }

    /**
     * 百度地图优先 → Haversine 降级
     */
    private long calculateWithFallback(BigDecimal lat1, BigDecimal lng1,
                                       BigDecimal lat2, BigDecimal lng2) {
        // 如果未配置百度 AK，直接用 Haversine
        if ("placeholder".equals(baiduAk) || baiduAk == null || baiduAk.isBlank()) {
            return haversineDist(lat1, lng1, lat2, lng2);
        }

        try {
            String url = distanceApi
                    + "?origin=" + lat1 + "," + lng1
                    + "&destination=" + lat2 + "," + lng2
                    + "&ak=" + baiduAk
                    + "&coord_type=wgs84";
            String response = restTemplate.getForObject(url, String.class);
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                int status = root.path("status").asInt();
                if (status == 0) {
                    // API 成功：提取距离（米）
                    JsonNode routes = root.path("result").path("routes");
                    if (routes.isArray() && routes.size() > 0) {
                        long distMeters = routes.get(0).path("distance").asLong();
                        log.debug("百度地图距离: ({},{})→({},{}) = {}m",
                                lat1, lng1, lat2, lng2, distMeters);
                        return distMeters;
                    }
                } else {
                    log.warn("百度地图 API 返回异常: status={}, msg={}",
                            status, root.path("message").asText());
                }
            }
        } catch (Exception e) {
            log.warn("百度地图 API 调用失败，降级为 Haversine: {}", e.getMessage());
        }

        // 降级：Haversine 公式
        return haversineDist(lat1, lng1, lat2, lng2);
    }

    private long haversineDist(BigDecimal lat1, BigDecimal lng1,
                               BigDecimal lat2, BigDecimal lng2) {
        double dist = HaversineUtil.distance(lat1, lng1, lat2, lng2);
        return Math.round(dist);
    }

}
