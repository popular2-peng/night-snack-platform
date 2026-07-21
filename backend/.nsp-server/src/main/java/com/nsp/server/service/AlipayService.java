package com.nsp.server.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付宝沙箱服务 — 扫码预下单（precreate）+ 回调验签
 *
 * <p>密钥全部走环境变量 ${}，不硬编码
 */
@Service
public class AlipayService {

    private static final Logger log = LoggerFactory.getLogger(AlipayService.class);

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.private-key}")
    private String privateKey;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.notify-url}")
    private String notifyUrl;

    @Value("${alipay.gateway-url}")
    private String gatewayUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AlipayClient getClient() {
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                "json",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
        );
    }

    /**
     * 扫码预下单 — 返回支付二维码链接
     *
     * @param orderNo   商户订单号
     * @param subject   商品标题
     * @param totalAmount 金额（元）
     * @return 支付宝返回的二维码链接（qr_code）
     */
    public String precreate(String orderNo, String subject, String totalAmount) {
        try {
            AlipayClient client = getClient();
            AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
            request.setNotifyUrl(notifyUrl);

            Map<String, Object> bizContent = Map.of(
                    "out_trade_no", orderNo,
                    "total_amount", totalAmount,
                    "subject", subject
            );
            request.setBizContent(objectMapper.writeValueAsString(bizContent));

            AlipayTradePrecreateResponse response = client.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝预下单成功: orderNo={}, qrCode={}", orderNo, response.getQrCode());
                return response.getQrCode();
            } else {
                log.error("支付宝预下单失败: code={}, msg={}", response.getCode(), response.getMsg());
                throw new BizException(ErrorCode.PAY_CHANNEL_ERROR,
                        "支付宝预下单失败: " + response.getMsg());
            }
        } catch (AlipayApiException e) {
            log.error("支付宝 API 异常", e);
            throw new BizException(ErrorCode.PAY_CHANNEL_ERROR, "支付宝通道异常: " + e.getErrMsg());
        } catch (Exception e) {
            log.error("支付宝预下单异常", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "支付服务异常");
        }
    }

    /**
     * 校验支付宝回调签名（RSA2）
     *
     * @param params 回调参数 Map
     * @return true 验签通过
     */
    public boolean verifySign(Map<String, String> params) {
        try {
            boolean valid = AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2");
            if (valid) {
                log.info("支付宝回调验签通过: orderNo={}", params.get("out_trade_no"));
            } else {
                log.warn("支付宝回调验签失败: orderNo={}", params.get("out_trade_no"));
            }
            return valid;
        } catch (AlipayApiException e) {
            log.error("支付宝验签异常", e);
            return false;
        }
    }

}
