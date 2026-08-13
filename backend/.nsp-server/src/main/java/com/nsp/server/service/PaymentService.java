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

    /**
     * 退款 — 按订单号对成功支付发起退款（渠道分发：ALIPAY 沙箱真实退款 / WECHAT_MOCK mock 退款）
     *
     * <p>幂等：同一订单已退款（status=REFUNDED）时直接返回；
     * 退款失败保持 refund_status=REFUNDING，可再次调用重试（复用同一退款请求号）。
     * 沙箱中未真实支付的订单（渠道无交易）降级为本地标记退款，保证演示链路完整。
     *
     * @param orderNo 订单号
     * @throws BizException 5005 无成功支付记录 / 5007 退款调用失败
     */
    void refund(String orderNo);

}
