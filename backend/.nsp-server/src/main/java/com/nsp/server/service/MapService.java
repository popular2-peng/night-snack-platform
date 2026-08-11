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

    /**
     * 地理编码：地址 → 经纬度（百度地图 Web API）
     *
     * <p>入驻申请时把「省 + 市 + 区县 + 详细地址」转成坐标，
     * 后续下单配送范围校验、距离计算使用。
     *
     * @param province 省
     * @param city     市
     * @param district 区县
     * @param detail   详细地址
     * @return 长度为 2 的数组 [lat, lng]；未配置 AK 或解析失败返回 {@code null}（调用方降级处理）
     */
    BigDecimal[] geocode(String province, String city, String district, String detail);

}
