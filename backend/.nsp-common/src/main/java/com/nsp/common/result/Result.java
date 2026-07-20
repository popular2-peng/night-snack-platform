package com.nsp.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体 — 所有接口返回此结构，便于前端和自动化测试统一解析
 *
 * <pre>{@code
 *   // 成功响应
 *   { "code": 0, "msg": "ok", "data": {...} }
 *   // 失败响应
 *   { "code": 2001, "msg": "用户名已存在", "data": null }
 * }</pre>
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 业务状态码，0 表示成功，非 0 表示失败 */
    private int code;

    /** 提示信息，成功时固定 "ok"，失败时描述错误原因 */
    private String msg;

    /** 业务数据，成功时返回，失败时为 null */
    private T data;

    // ──────────────────────────────────────
    // 静态工厂方法
    // ──────────────────────────────────────

    /** 成功响应（带数据） */
    public static <T> Result<T> ok(T data) {
        return new Result<>(0, "ok", data);
    }

    /** 成功响应（无数据，如删除操作） */
    public static <T> Result<T> ok() {
        return new Result<>(0, "ok", null);
    }

    /** 失败响应 */
    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    /** 失败响应（带数据，少数场景需要） */
    public static <T> Result<T> fail(int code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

}
