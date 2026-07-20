package com.nsp.common.exception;

import lombok.Getter;

/**
 * 业务异常 — 业务层遇到非预期情况时抛出，由 GlobalExceptionHandler 统一捕获并转为 Result
 *
 * <p>用法：
 * <pre>{@code
 *   throw new BizException(ErrorCode.USER_NOT_FOUND);
 *   throw new BizException(2001, "用户名已存在");
 * }</pre>
 *
 * <p>区别于系统异常（NPE、IO 等）：BizException 是"可预期的业务失败"，message 可直接返回前端；
 * 系统异常统一返回"系统繁忙，请稍后再试"。
 */
@Getter
public class BizException extends RuntimeException {

    /** 业务错误码（对齐 ErrorCode 常量） */
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 使用 ErrorCode 常量创建异常，message 自动填充 */
    public BizException(int code) {
        super("业务异常，错误码: " + code);
        this.code = code;
    }

}
