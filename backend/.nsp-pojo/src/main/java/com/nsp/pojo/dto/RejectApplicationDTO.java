package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 拒绝入驻申请请求体
 */
@Data
public class RejectApplicationDTO {

    /** 拒绝原因（必填，将展示给商家） */
    @NotBlank(message = "拒绝原因不能为空")
    private String rejectReason;

}
