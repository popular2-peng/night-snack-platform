package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 收货地址新增/编辑请求参数
 */
@Data
public class AddressDTO {

    @NotBlank(message = "收件人不能为空")
    private String receiver;

    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    @NotNull(message = "纬度不能为空")
    private BigDecimal lat;

    @NotNull(message = "经度不能为空")
    private BigDecimal lng;

    /** 是否设为默认地址 */
    private Boolean isDefault;

}
