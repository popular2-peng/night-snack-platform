package com.nsp.pojo.vo;

import lombok.Data;

/**
 * OSS 上传签名响应 — 前端凭此签名直传 OSS
 */
@Data
public class UploadSignVO {

    /** OSS access key id */
    private String accessKeyId;
    /** 上传策略（base64） */
    private String policy;
    /** 签名 */
    private String signature;
    /** OSS Host */
    private String host;
    /** 上传目录 */
    private String dir;
    /** 签名过期时间（秒） */
    private Long expire;

}
