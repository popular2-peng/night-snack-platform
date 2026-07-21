package com.nsp.server.service;

import com.nsp.pojo.dto.PayRequestDTO;
import com.nsp.pojo.vo.PayVO;

import java.util.Map;

/**
 * 支付业务接口 — 发起支付、查询、回调处理
 */
public interface PaymentService {

    /**
     * 发起支付
     */
    PayVO pay(Long userId, PayRequestDTO dto);

    /**
     * 支付宝回调处理（验签 + 幂等 + 更新订单状态）
     */
    void handleAlipayCallback(Map<String, String> params);

    /**
     * 查询支付状态
     */
    PayVO queryStatus(String orderNo);

}
