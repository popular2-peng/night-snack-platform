package com.nsp.server.controller;

import com.nsp.server.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调接口 — 匿名访问，白名单 IP 校验
 *
 * <p>POST /api/pay/alipay/callback 已在 SecurityConfig 中放行（无需 Token）
 */
@Tag(name = "支付回调", description = "支付宝异步通知回调")
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayCallbackController {

    private static final Logger log = LoggerFactory.getLogger(PayCallbackController.class);

    private final PaymentService paymentService;

    @Operation(summary = "支付宝支付回调（匿名）")
    @PostMapping("/alipay/callback")
    public String alipayCallback(HttpServletRequest request) {
        // 将支付宝回调参数转为 Map
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });

        log.info("收到支付宝回调: orderNo={}, tradeNo={}",
                params.get("out_trade_no"), params.get("trade_no"));

        try {
            paymentService.handleAlipayCallback(params);
            return "success"; // 支付宝要求返回 "success" 才停止重试
        } catch (Exception e) {
            log.error("支付宝回调处理失败", e);
            return "fail";
        }
    }

}
