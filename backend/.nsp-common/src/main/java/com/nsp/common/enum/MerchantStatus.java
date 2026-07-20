package com.nsp.common.enum;

import lombok.Getter;

/**
 * 商家状态
 */
@Getter
public enum MerchantStatus {

    /** 审核中 */
    PENDING("PENDING", "审核中"),

    /** 营业中 */
    OPEN("OPEN", "营业中"),

    /** 已打烊 */
    CLOSED("CLOSED", "已打烊"),

    /** 已封禁 */
    SUSPENDED("SUSPENDED", "已封禁");

    private final String code;
    private final String desc;

    MerchantStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MerchantStatus fromCode(String code) {
        for (MerchantStatus s : values()) {
            if (s.code.equalsIgnoreCase(code)) return s;
        }
        return null;
    }

}
