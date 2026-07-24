package com.nsp.server.aspect;

import com.nsp.server.annotation.Loggable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 商家操作日志切面 — 拦截 @Loggable 方法，记录操作人、目标、操作类型
 */
@Aspect
@Component
public class MerchantOperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(MerchantOperationLogAspect.class);

    @Around("@annotation(com.nsp.server.annotation.Loggable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Loggable ann = signature.getMethod().getAnnotation(Loggable.class);

        // 从参数中提取 targetId 和 operatorId
        Object[] args = joinPoint.getArgs();
        Object targetId = args.length > 0 ? args[0] : "?";
        Object operatorId = args.length > 1 ? args[1] : "SYSTEM";

        log.info("[OP_LOG] action={}, targetType={}, targetId={}, operatorId={}",
                ann.action(), ann.targetType(), targetId, operatorId);

        return joinPoint.proceed();
    }

}
