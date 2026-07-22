package com.nsp.pojo.dto;

import lombok.Data;

/**
 * 商家入驻审核请求体
 */
@Data
public class AuditDTO {

    /** 拒绝理由（拒绝时必填） */
    private String reason;

}
