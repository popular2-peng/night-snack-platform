package com.nsp.common.util;

import java.math.BigDecimal;

/**
 * Haversine 公式工具 — 计算两点间地表距离（米）
 *
 * <p>用于商家配送范围校验的降级方案（百度地图 API 不可用时）
 * <p>地球平均半径取 6371 km，结果精确到米
 */
public final class HaversineUtil {

    private HaversineUtil() { /* 工具类禁止实例化 */ }

    /** 地球平均半径（公里） */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * 计算两点间距离（米）
     *
     * @param lat1 纬度 1
     * @param lng1 经度 1
     * @param lat2 纬度 2
     * @param lng2 经度 2
     * @return 距离（米）
     */
    public static double distance(BigDecimal lat1, BigDecimal lng1,
                                  BigDecimal lat2, BigDecimal lng2) {
        return distance(
                lat1.doubleValue(), lng1.doubleValue(),
                lat2.doubleValue(), lng2.doubleValue());
    }

    /**
     * 计算两点间距离（米）
     *
     * @param lat1 纬度 1（度）
     * @param lng1 经度 1（度）
     * @param lat2 纬度 2（度）
     * @param lng2 经度 2（度）
     * @return 距离（米）
     */
    public static double distance(double lat1, double lng1,
                                  double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c * 1000; // 公里 → 米
    }

}
