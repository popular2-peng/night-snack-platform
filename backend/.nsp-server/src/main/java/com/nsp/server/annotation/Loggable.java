package com.nsp.server.annotation;

import java.lang.annotation.*;

/**
 * 商家操作日志标记 — 标注在需要记录操作日志的方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {

    /** 操作类型，如"封禁商家""通过入驻" */
    String action();

    /** 目标类型 */
    String targetType() default "merchant";

}
