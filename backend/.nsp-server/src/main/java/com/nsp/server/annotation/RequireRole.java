package com.nsp.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解 — 标注在 Controller 类或方法上
 * <p>类上的注解对该类所有方法生效；方法上的注解覆盖类级别注解
 *
 * <p>校验逻辑由 {@code RoleAspect} 切面完成
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许的角色列表，满足其中一个即可 */
    String[] value();

}
