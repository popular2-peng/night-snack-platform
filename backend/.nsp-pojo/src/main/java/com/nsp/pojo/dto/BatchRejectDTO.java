package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量拒绝入驻申请请求体
 */
@Data
public class BatchRejectDTO {

    @NotEmpty(message = "请至少选择一条申请")
    private List<Long> ids;

    /** 统一拒绝原因（必填，10-200 字，展示给商家） */
    @NotBlank(message = "拒绝原因不能为空")
    @Size(min = 10, max = 200, message = "拒绝原因需 10-200 字")
    private String rejectReason;

}
