package com.nsp.server.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 商家 WebSocket 会话管理 — merchantId → 该商家当前所有在线连接
 *
 * <p>一个商家可多端在线（多开浏览器），新订单推送到该商家的全部会话；
 * 会话断开由 handler 回调注销，保证 Map 不泄漏。
 */
@Component
public class MerchantWsSessionManager {

    private static final Logger log = LoggerFactory.getLogger(MerchantWsSessionManager.class);

    private final Map<Long, Set<WebSocketSession>> sessionsByMerchant = new ConcurrentHashMap<>();

    /** 连接建立时注册会话 */
    public void register(Long merchantId, WebSocketSession session) {
        sessionsByMerchant.computeIfAbsent(merchantId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("商家 WS 连接建立: merchantId={}, sessionId={}, 当前在线会话={}",
                merchantId, session.getId(), sessionsByMerchant.get(merchantId).size());
    }

    /** 连接关闭/异常时注销会话 */
    public void unregister(Long merchantId, WebSocketSession session) {
        Set<WebSocketSession> set = sessionsByMerchant.get(merchantId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                sessionsByMerchant.remove(merchantId);
            }
        }
        log.info("商家 WS 连接关闭: merchantId={}, sessionId={}", merchantId, session.getId());
    }

    /**
     * 向商家的全部在线会话推送消息（JSON 文本帧）
     *
     * <p>单个会话发送失败仅告警并跳过，不影响其他会话。
     */
    public void sendToMerchant(Long merchantId, String payload) {
        Set<WebSocketSession> set = sessionsByMerchant.get(merchantId);
        if (set == null || set.isEmpty()) {
            log.debug("商家无在线 WS 会话，跳过推送: merchantId={}", merchantId);
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (WebSocketSession session : set) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                }
            } catch (IOException | IllegalStateException e) {
                log.warn("WS 推送失败（跳过该会话）: merchantId={}, sessionId={}, err={}",
                        merchantId, session.getId(), e.getMessage());
            }
        }
        log.info("新订单推送完成: merchantId={}, 会话数={}", merchantId, set.size());
    }

}
