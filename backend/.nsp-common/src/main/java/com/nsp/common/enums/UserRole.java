package com.nsp.common.enums;

import lombok.Getter;

/**
 * 用户角色 — 用于 JWT claims 中的角色标识和 {@code @RequireRole} 注解校验
 */
@Getter
public enum UserRole {

    /** 普通用户（移动端） */
    USER("USER", "普通用户"),

    /** 商家 */
    MERCHANT("MERCHANT", "商家"),

    /** 平台管理员 */
    ADMIN("ADMIN", "管理员");

    private final String code;
    private final String desc;

    UserRole(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 根据 code 字符串查找枚举，找不到返回 null */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }

}
