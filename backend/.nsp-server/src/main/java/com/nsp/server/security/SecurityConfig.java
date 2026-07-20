package com.nsp.server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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

                        // 用户端：登录/注册公开
                        .requestMatchers(
                                "/api/user/login",
                                "/api/user/register"
                        ).permitAll()

                        // 支付回调 / OSS 回调（Phase 2）
                        .requestMatchers(
                                "/api/pay/alipay/callback",
                                "/api/upload/callback"
                        ).permitAll()

                        // 其余接口全部需要认证
                        .anyRequest().authenticated()
                )

                // JWT 过滤器插在 UsernamePasswordAuthenticationFilter 之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
