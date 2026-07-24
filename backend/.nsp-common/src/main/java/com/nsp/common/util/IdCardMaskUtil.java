package com.nsp.common.util;

/**
 * 身份证号脱敏工具 — 410***********1234
 */
public final class IdCardMaskUtil {

    private IdCardMaskUtil() {}

    public static String mask(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

}
