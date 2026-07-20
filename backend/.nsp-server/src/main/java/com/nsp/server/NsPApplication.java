package com.nsp.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 夜宵外卖平台启动类
 *
 * <p>启动后访问：
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8080/swagger-ui.html">http://localhost:8080/swagger-ui.html</a></li>
 *   <li>API 文档 JSON: <a href="http://localhost:8080/v3/api-docs">http://localhost:8080/v3/api-docs</a></li>
 * </ul>
 *
 * <p>端口和数据库等配置见 {@code application.yml} 及环境变量
 */
@SpringBootApplication(scanBasePackages = "com.nsp")
@MapperScan("com.nsp.server.mapper")
public class NsPApplication {

    public static void main(String[] args) {
        SpringApplication.run(NsPApplication.class, args);
    }

}
