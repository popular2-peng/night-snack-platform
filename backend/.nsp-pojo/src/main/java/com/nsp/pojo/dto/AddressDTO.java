package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "详细地址不能为空")
    private String detail;

    /** 纬度（可选）：移动端暂不支持地图选点，为空时下单距离校验按 0 处理（MVP 降级） */
    private BigDecimal lat;

    /** 经度（可选）：同 lat */
    private BigDecimal lng;

    /** 是否设为默认地址 */
    private Boolean isDefault;

}
