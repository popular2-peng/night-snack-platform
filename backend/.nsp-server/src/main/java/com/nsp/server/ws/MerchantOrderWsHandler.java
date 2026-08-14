package com.nsp.server.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

/**
 * 商家订单 WebSocket 处理器 — 文本帧 JSON 协议
 *
 * <p>服务端推送：
 * <ul>
 *   <li>{@code {"type":"CONNECTED"}} — 连接建立成功（认证通过）</li>
 *   <li>{@code {"type":"PONG"}} — 心跳响应</li>
 *   <li>{@code {"type":"NEW_ORDER","data":{orderId,orderNo,totalAmount,createdAt}} — 支付成功新订单</li>
 * </ul>
 * <p>客户端上行：{@code {"type":"PING"}} 心跳（建议 30 秒一次）。
 */
@Component
@RequiredArgsConstructor
public class MerchantOrderWsHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MerchantOrderWsHandler.class);

    /** merchantId 由握手拦截器认证后写入 session attributes */
    public static final String ATTR_MERCHANT_ID = "merchantId";

    private final MerchantWsSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long merchantId = (Long) session.getAttributes().get(ATTR_MERCHANT_ID);
        sessionManager.register(merchantId, session);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                Map.of("type", "CONNECTED"))));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 心跳：PING → PONG（客户端维持长连接用）
        if (message.getPayload() != null && message.getPayload().contains("\"PING\"")) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    Map.of("type", "PONG"))));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long merchantId = (Long) session.getAttributes().get(ATTR_MERCHANT_ID);
        if (merchantId != null) {
            sessionManager.unregister(merchantId, session);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("商家 WS 传输异常: sessionId={}, err={}", session.getId(), exception.getMessage());
        Long merchantId = (Long) session.getAttributes().get(ATTR_MERCHANT_ID);
        if (merchantId != null) {
            sessionManager.unregister(merchantId, session);
        }
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (Exception ignored) {
            // 关闭失败忽略
        }
    }

}
