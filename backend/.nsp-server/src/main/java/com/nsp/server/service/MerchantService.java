package com.nsp.server.service;

import com.nsp.pojo.vo.MerchantVO;

import java.util.List;

/**
 * 商家业务接口
 */
public interface MerchantService {

    /**
     * 查询附近商家（按距离排序）
     *
     * @param lat 用户纬度
     * @param lng 用户经度
     * @return 商家列表（含距离）
     */
    List<MerchantVO> listNearby(java.math.BigDecimal lat, java.math.BigDecimal lng);

    /**
     * 查询商家详情
     */
    MerchantVO getById(Long id);

    /**
     * 更新营业状态
     */
    void updateStatus(Long merchantId, Long userId, String status);

}
