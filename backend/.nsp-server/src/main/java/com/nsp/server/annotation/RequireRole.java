package com.nsp.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限注解 — 标注在 Controller 方法上，声明该方法需要哪些角色才能访问
 *
 * <p>用法：
 * <pre>{@code
 *   @RequireRole("ADMIN")
 *   @GetMapping("/merchant/audit")
 *   public Result<?> audit() { ... }
 *
 *   @RequireRole({"USER", "MERCHANT", "ADMIN"})
 *   @PostMapping("/upload/sign")
 *   public Result<?> getUploadSign() { ... }
 * }</pre>
 *
 * <p>校验逻辑由 {@code RoleAspect} 切面完成
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** 允许的角色列表，满足其中一个即可 */
    String[] value();

}
