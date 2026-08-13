package com.nsp.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401 统一响应入口 — 未认证/Token 异常的请求返回 Result JSON（而非 Spring 默认响应体）
 *
 * <p>失败原因由 {@link JwtAuthenticationFilter} 写入 request attribute（见下方常量），
 * 便于前端和 pytest 区分：未登录 1001 / Token 过期 1003 / Token 无效 1004。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** request attribute 名：认证失败原因 */
    public static final String ERROR_ATTRIBUTE = "auth.error";
    public static final String ERROR_MISSING = "MISSING";       // 无 Token
    public static final String ERROR_EXPIRED = "EXPIRED";       // Token 已过期
    public static final String ERROR_INVALID = "INVALID";       // 签名错误/格式错误
    public static final String ERROR_REFRESH_TYPE = "REFRESH_TYPE"; // 刷新令牌用于业务接口

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String error = (String) request.getAttribute(ERROR_ATTRIBUTE);
        if (error == null) {
            error = ERROR_MISSING;
        }

        int code;
        String msg;
        switch (error) {
            case ERROR_EXPIRED -> {
                code = ErrorCode.TOKEN_EXPIRED;
                msg = MessageConst.TOKEN_EXPIRED_MSG;
            }
            case ERROR_INVALID, ERROR_REFRESH_TYPE -> {
                code = ErrorCode.TOKEN_INVALID;
                msg = MessageConst.TOKEN_INVALID_MSG;
            }
            default -> {
                code = ErrorCode.UNAUTHORIZED;
                msg = MessageConst.TOKEN_UNAUTHORIZED_MSG;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.fail(code, msg));
    }

}
