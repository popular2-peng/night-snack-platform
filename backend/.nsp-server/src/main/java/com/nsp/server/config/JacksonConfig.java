package com.nsp.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置 — 统一 LocalDateTime 序列化格式
 *
 * <p>规范（CLAUDE.md §15）：所有时间字段统一 ISO-8601 格式 {@code yyyy-MM-dd'T'HH:mm:ss}。
 * 显式注册序列化器可防止出现纳秒尾巴（如 {@code 21:30:00.123456}）导致格式不统一。
 * LocalTime（营业时间）与 LocalDate（日期字段）仍由各 VO 的 @JsonFormat 控制，不受此配置影响。
 */
@Configuration
public class JacksonConfig {

    /** 统一时间格式：ISO-8601（带 T） */
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(LOCAL_DATE_TIME_FORMATTER));
        return builder.modules(javaTimeModule).build();
    }

}
