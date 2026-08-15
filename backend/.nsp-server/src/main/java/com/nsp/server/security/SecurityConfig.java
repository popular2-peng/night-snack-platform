package com.nsp.server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置 — JWT 无状态认证
 *
 * <p>白名单（无需 Token 即可访问）：
 * <ul>
 *   <li>用户登录/注册</li>
 *   <li>Swagger / SpringDoc 文档页面</li>
 *   <li>支付宝回调 / OSS 回调（Phase 2）</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（无 Cookie Session）
                .csrf(csrf -> csrf.disable())

                // 无状态，不创建 Session
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 路由权限
                .authorizeHttpRequests(auth -> auth
                        // Swagger / SpringDoc
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 健康检查（运维探活，无需鉴权；只暴露 health 端点）
                        .requestMatchers("/actuator/health").permitAll()

                        // WebSocket 握手（/ws/merchant）：握手鉴权由 WsHandshakeInterceptor 校验 token
                        .requestMatchers("/ws/**").permitAll()

                        // 用户端：登录/注册公开
                        .requestMatchers(
                                "/api/user/login",
                                "/api/user/register"
                        ).permitAll()

                        // 用户端浏览接口（商家/商品/分类，匿名公开）
                        .requestMatchers(HttpMethod.GET,
                                "/api/user/categories",
                                "/api/user/merchants",
                                "/api/user/merchants/**"
                        ).permitAll()

                        // 商家端：登录 + 入驻申请（注册）+ 注册页品类下拉 + 注册页 OSS 签名（匿名直传店铺 LOGO）
                        .requestMatchers(
                                "/api/merchant/login",
                                "/api/merchant/register",
                                "/api/merchant/category/all",
                                "/api/upload/sign"
                        ).permitAll()

                        // 管理端登录
                        .requestMatchers("/api/admin/login").permitAll()

                        // Token 刷新（三端通用，refresh token 本身即凭证，接口内部校验）
                        .requestMatchers("/api/auth/refresh").permitAll()

                        // 支付回调 / OSS 回调（Phase 2）
                        .requestMatchers(
                                "/api/pay/alipay/callback",
                                "/api/upload/callback"
                        ).permitAll()

                        // 其余接口全部需要认证
                        .anyRequest().authenticated()
                )

                // 401 统一响应：Result JSON（1001/1003/1004），替代 Spring 默认响应体
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))

                // JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
