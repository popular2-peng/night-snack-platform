package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 上传绑定请求参数 — 前端直传 OSS 成功后回传 ossKey 关联业务对象
 */
@Data
public class UploadBindDTO {

    @NotBlank(message = "OSS Key 不能为空")
    private String ossKey;

    @NotBlank(message = "业务类型不能为空（AVATAR/SHOP_LOGO/PRODUCT_IMAGE）")
    private String bizType;

    /** 关联业务对象 ID（如 productId） */
    private Long targetId;

}
