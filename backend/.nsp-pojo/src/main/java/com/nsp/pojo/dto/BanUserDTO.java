package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 封禁用户请求体
 */
@Data
public class BanUserDTO {

    /** 封禁原因（必填） */
    @NotBlank(message = "封禁原因不能为空")
    private String reason;

    /** 封禁时长（天）：null 表示永久封禁 */
    private Integer durationDays;

}
