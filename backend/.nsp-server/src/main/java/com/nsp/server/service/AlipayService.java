package com.nsp.server.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
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
     * 支付宝沙箱是否已配置（app-id / 私钥非 placeholder）
     *
     * <p>未配置时支付通道降级为 mock 直接成功（与地图 AK 未配置降级同模式），
     * 配置真实沙箱密钥（环境变量 ALIPAY_APP_ID / ALIPAY_PRIVATE_KEY / ALIPAY_PUBLIC_KEY）后自动走真实链路。
     */
    public boolean isConfigured() {
        return appId != null && !"placeholder".equals(appId)
                && privateKey != null && !"placeholder".equals(privateKey);
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

    /**
     * 支付宝退款（沙箱同步返回）
     *
     * <p>out_request_no 为商户侧退款请求号：同一请求号重复调用时，
     * 支付宝直接返回上一次结果，天然幂等 —— 调用方必须保证重试时复用同一请求号。
     *
     * @param orderNo      商户订单号（原支付 out_trade_no）
     * @param outRefundNo  退款请求号（商户侧生成并持久化）
     * @param refundAmount 退款金额（元，两位小数，≤ 原支付金额）
     * @throws BizException 5006 渠道无真实交易（沙箱未真实支付）；5007 其他退款失败
     */
    public void refund(String orderNo, String outRefundNo, String refundAmount) {
        try {
            AlipayClient client = getClient();
            AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

            Map<String, Object> bizContent = Map.of(
                    "out_trade_no", orderNo,
                    "out_request_no", outRefundNo,
                    "refund_amount", refundAmount
            );
            request.setBizContent(objectMapper.writeValueAsString(bizContent));

            AlipayTradeRefundResponse response = client.execute(request);
            if (response.isSuccess()) {
                log.info("支付宝退款成功: orderNo={}, outRefundNo={}, amount={}, tradeNo={}",
                        orderNo, outRefundNo, refundAmount, response.getTradeNo());
                return;
            }

            // 沙箱中未真实支付的订单在支付宝侧不存在交易 → 交由上层降级处理
            if ("ACQ.TRADE_NOT_EXIST".equals(response.getCode())) {
                log.warn("支付宝退款失败-渠道无真实交易: orderNo={}, code={}, msg={}",
                        orderNo, response.getCode(), response.getMsg());
                throw new BizException(ErrorCode.REFUND_TRADE_NOT_EXIST,
                        MessageConst.REFUND_TRADE_NOT_EXIST_MSG);
            }
            log.error("支付宝退款失败: orderNo={}, code={}, msg={}",
                    orderNo, response.getCode(), response.getMsg());
            throw new BizException(ErrorCode.REFUND_FAILED, MessageConst.REFUND_FAILED_MSG);
        } catch (BizException e) {
            throw e;
        } catch (AlipayApiException e) {
            log.error("支付宝退款 API 异常: orderNo={}", orderNo, e);
            throw new BizException(ErrorCode.REFUND_FAILED, MessageConst.REFUND_FAILED_MSG);
        } catch (Exception e) {
            log.error("支付宝退款异常: orderNo={}", orderNo, e);
            throw new BizException(ErrorCode.REFUND_FAILED, MessageConst.REFUND_FAILED_MSG);
        }
    }

}
