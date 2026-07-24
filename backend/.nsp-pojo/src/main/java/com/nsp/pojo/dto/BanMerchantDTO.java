package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 封禁商家请求体
 */
@Data
public class BanMerchantDTO {

    /** 封禁原因（必填） */
    @NotBlank(message = "封禁原因不能为空")
    private String reason;

}
