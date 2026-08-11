package com.nsp.pojo.dto;

import lombok.Data;

/**
 * 通过入驻申请请求体
 *
 * <p>配送半径不在审核时设置：商家审核通过后自行在店铺设置中配置。
 */
@Data
public class ApproveApplicationDTO {

    /** 初始营业状态：OPEN / CLOSED，默认 OPEN */
    private String initialStatus;

    /** 审核备注（可选） */
    private String reviewNote;

}
