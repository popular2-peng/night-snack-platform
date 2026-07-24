package com.nsp.common.util;

/**
 * 手机号脱敏工具 — 138****0002
 */
public final class PhoneMaskUtil {

    private PhoneMaskUtil() {}

    public static String mask(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

}
