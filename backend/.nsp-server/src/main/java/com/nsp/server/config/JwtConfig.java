package com.nsp.server.config;

import com.nsp.common.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置 — 将 JwtUtil 实例注入 Spring 容器
 */
@Configuration
public class JwtConfig {

    @Value("${nsp.jwt.secret}")
    private String jwtSecret;

    /**
     * 创建 JwtUtil Bean，密钥从配置文件注入（配置文件值来自环境变量 ${JWT_SECRET}）
     */
    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil(jwtSecret);
    }

}
