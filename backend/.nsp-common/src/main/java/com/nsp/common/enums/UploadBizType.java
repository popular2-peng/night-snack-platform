package com.nsp.common.enums;

import lombok.Getter;

/**
 * 上传业务类型 — 决定 OSS 存储目录和关联业务对象
 */
@Getter
public enum UploadBizType {

    /** 用户头像 */
    AVATAR("AVATAR", "用户头像", "avatar"),

    /** 店铺头像/Logo */
    SHOP_LOGO("SHOP_LOGO", "店铺头像", "shop"),

    /** 商品图片 */
    PRODUCT_IMAGE("PRODUCT_IMAGE", "商品图片", "product");

    private final String code;
    private final String desc;
    /** OSS 存储目录前缀 */
    private final String dirPrefix;

    UploadBizType(String code, String desc, String dirPrefix) {
        this.code = code;
        this.desc = desc;
        this.dirPrefix = dirPrefix;
    }

    public static UploadBizType fromCode(String code) {
        for (UploadBizType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        return null;
    }

}
