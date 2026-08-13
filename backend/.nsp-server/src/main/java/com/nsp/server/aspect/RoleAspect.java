package com.nsp.server.aspect;

import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.server.annotation.RequireRole;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色权限切面 — 拦截 @RequireRole 注解，校验当前用户角色
 */
@Aspect
@Component
public class RoleAspect {

    private static final Logger log = LoggerFactory.getLogger(RoleAspect.class);

    @Before("@annotation(com.nsp.server.annotation.RequireRole) "
            + "|| @within(com.nsp.server.annotation.RequireRole)")
    public void checkRole(JoinPoint joinPoint) {
        // 优先取方法级注解，其次取类级注解
        RequireRole requireRole = getRequireRole(joinPoint);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("未登录访问受保护接口: {}", joinPoint.getSignature());
            throw new BizException(ErrorCode.UNAUTHORIZED, MessageConst.TOKEN_UNAUTHORIZED_MSG);
        }

        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        String[] requiredRoles = requireRole.value();

        boolean hasRole = Arrays.stream(requiredRoles)
                .anyMatch(role -> userRoles.contains("ROLE_" + role));

        if (!hasRole) {
            log.warn("权限不足: 用户角色={}, 需要角色={}, 接口={}",
                    userRoles, Arrays.toString(requiredRoles), joinPoint.getSignature());
            throw new BizException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

    /**
     * 从方法或类上获取 @RequireRole 注解（方法优先）
     */
    private RequireRole getRequireRole(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole methodAnnotation = method.getAnnotation(RequireRole.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return joinPoint.getTarget().getClass().getAnnotation(RequireRole.class);
    }

}
