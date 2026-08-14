package com.nsp.server.ws;

import com.nsp.common.jwt.JwtUtil;
import com.nsp.server.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * WebSocket 握手鉴权 — 校验 query 中的 JWT
 *
 * <p>连接地址：{@code ws://host/ws/merchant?token=<accessToken>}。
 * 校验通过后把 merchantId（JWT sub）写入 session attributes，供 handler 注册会话；
 * 校验失败返回 false（握手拒绝）。
 */
@Component
@RequiredArgsConstructor
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WsHandshakeInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            log.warn("WS 握手拒绝：缺少 token，path={}", request.getURI().getPath());
            return false;
        }
        // 必须是有效 ACCESS 令牌（refresh 令牌禁止用于业务连接）
        if (!jwtTokenProvider.validateToken(token)
                || JwtUtil.TOKEN_TYPE_REFRESH.equals(jwtTokenProvider.getTokenType(token))) {
            log.warn("WS 握手拒绝：token 无效或为刷新令牌, path={}", request.getURI().getPath());
            return false;
        }
        // 仅商家角色可连接（新订单推送对象是商家）
        if (!"MERCHANT".equals(jwtTokenProvider.getRole(token))) {
            log.warn("WS 握手拒绝：非商家角色, path={}", request.getURI().getPath());
            return false;
        }
        attributes.put(MerchantOrderWsHandler.ATTR_MERCHANT_ID, jwtTokenProvider.getUserId(token));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需后置处理
    }

    /** 从 query 中提取 token 参数（如 ?token=xxx&other=y） */
    private String extractToken(ServerHttpRequest request) {
        String query = request.getURI().getRawQuery();
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && "token".equals(pair.substring(0, idx))) {
                return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

}
