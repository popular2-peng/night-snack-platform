package com.nsp.pojo.dto;

import lombok.Data;

/**
 * 通过入驻申请请求体
 */
@Data
public class ApproveApplicationDTO {

    /** 初始营业状态：OPEN / CLOSED，默认 OPEN */
    private String initialStatus;

    /** 配送半径（米） */
    private Integer deliveryRadius;

    /** 审核备注（可选） */
    private String reviewNote;

}
