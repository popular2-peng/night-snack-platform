package com.nsp.common.enum;

import lombok.Getter;

/**
 * 支付渠道
 */
@Getter
public enum PayChannel {

    /** 支付宝（沙箱） */
    ALIPAY("ALIPAY", "支付宝"),

    /** 微信支付（mock 通道，后续替换为正式 JSAPI） */
    WECHAT_MOCK("WECHAT_MOCK", "微信支付");

    private final String code;
    private final String desc;

    PayChannel(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PayChannel fromCode(String code) {
        for (PayChannel c : values()) {
            if (c.code.equalsIgnoreCase(code)) return c;
        }
        return null;
    }

}
