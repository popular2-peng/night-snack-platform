package com.nsp.server.service;

import java.math.BigDecimal;

/**
 * 地图服务接口 — 距离计算、地理编码
 *
 * <p>Phase 2.1 先用 Haversine 实现；Phase 2.3 接入百度地图 API
 */
public interface MapService {

    /**
     * 计算两点间距离（米）
     *
     * @param lat1 纬度 1
     * @param lng1 经度 1
     * @param lat2 纬度 2
     * @param lng2 经度 2
     * @return 距离（米）
     */
    long calculateDistance(BigDecimal lat1, BigDecimal lng1,
                           BigDecimal lat2, BigDecimal lng2);

}
