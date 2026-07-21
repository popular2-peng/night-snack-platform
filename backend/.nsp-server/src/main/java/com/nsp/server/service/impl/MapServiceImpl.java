package com.nsp.server.service.impl;

import com.nsp.common.util.HaversineUtil;
import com.nsp.server.service.MapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 地图服务实现 — 当前使用 Haversine 公式
 *
 * <p>Phase 2.3: 优先调用百度地图 Web API，失败时降级为 Haversine
 */
@Service
public class MapServiceImpl implements MapService {

    private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

    @Override
    public long calculateDistance(BigDecimal lat1, BigDecimal lng1,
                                  BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            log.warn("坐标参数为空，返回默认距离 0");
            return 0;
        }
        double dist = HaversineUtil.distance(lat1, lng1, lat2, lng2);
        long result = Math.round(dist);
        log.debug("Haversine 距离: ({},{}) → ({},{}) = {}m", lat1, lng1, lat2, lng2, result);
        return result;
    }

}
