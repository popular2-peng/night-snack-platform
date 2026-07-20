package com.nsp.server.aspect;

import com.nsp.common.constant.ErrorCode;
import com.nsp.common.exception.BizException;
import com.nsp.server.annotation.RequireRole;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色权限切面 — 拦截 @RequireRole 注解，校验当前用户角色
 *
 * <p>校验逻辑：
 * <ol>
 *   <li>从 SecurityContextHolder 取出当前认证信息</li>
 *   <li>如果未认证 → 抛出 BizException(UNAUTHORIZED, 1001)</li>
 *   <li>遍历用户的 authorities，检查是否命中 @RequireRole 中声明的任一角色</li>
 *   <li>不满足 → 抛出 BizException(FORBIDDEN, 1002)</li>
 * </ol>
 */
@Slf4j
@Aspect
@Component
public class RoleAspect {

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 未登录
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("未登录访问受保护接口: {}", joinPoint.getSignature());
            throw new BizException(ErrorCode.UNAUTHORIZED, "请先登录");
        }

        // 收集当前用户的角色
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // 所需角色列表
        String[] requiredRoles = requireRole.value();

        // 检查是否命中任一所需角色
        boolean hasRole = Arrays.stream(requiredRoles)
                .anyMatch(role -> userRoles.contains("ROLE_" + role));

        if (!hasRole) {
            log.warn("权限不足: 用户角色={}, 需要角色={}, 接口={}",
                    userRoles, Arrays.toString(requiredRoles), joinPoint.getSignature());
            throw new BizException(ErrorCode.FORBIDDEN, "权限不足");
        }
    }

}
