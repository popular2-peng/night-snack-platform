package com.nsp.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 微信支付 Mock 通道 — 直接返回支付成功
 *
 * <p>Phase 2 MVP 阶段用 mock 代替真实微信 JSAPI，保留接口抽象方便后续替换
 */
@Service
public class WechatMockService {

    private static final Logger log = LoggerFactory.getLogger(WechatMockService.class);

    /**
     * Mock 支付 — 直接返回成功
     *
     * @param orderNo 订单号
     * @return 固定返回 "MOCK_PAID"
     */
    public String mockPay(String orderNo) {
        log.info("微信 Mock 支付: orderNo={} → 直接成功", orderNo);
        return "MOCK_PAID";
    }

    /**
     * Mock 退款 — 直接返回成功（保留渠道抽象，正式接入微信退款时替换实现）
     *
     * @param orderNo 订单号
     */
    public void mockRefund(String orderNo) {
        log.info("微信 Mock 退款: orderNo={} → 直接成功", orderNo);
    }

}
