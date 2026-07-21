package com.nsp.server.service;

import java.math.BigDecimal;

/**
 * 地图服务接口 — 距离计算、地理编码
 *
 * <p>Redis 缓存 + 百度地图优先 + Haversine 降级
 */
public interface MapService {

    /**
     * 计算两点间距离（米）— 无缓存
     */
    long calculateDistance(BigDecimal lat1, BigDecimal lng1,
                           BigDecimal lat2, BigDecimal lng2);

    /**
     * 计算距离并缓存（用于附近商家列表，同一用户短时间内重复请求不重复调 API）
     *
     * @param shopLat  商家纬度
     * @param shopLng  商家经度
     * @param userLat  用户纬度
     * @param userLng  用户经度
     * @param shopId   商家 ID
     * @return 距离（米）
     */
    long calculateDistanceWithCache(BigDecimal shopLat, BigDecimal shopLng,
                                    BigDecimal userLat, BigDecimal userLng,
                                    Long shopId);

}
