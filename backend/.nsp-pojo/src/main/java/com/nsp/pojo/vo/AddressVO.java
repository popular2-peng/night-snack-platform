package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收货地址响应
 */
@Data
public class AddressVO {

    private Long id;
    private String receiver;
    private String phone;
    private String detail;
    private BigDecimal lat;
    private BigDecimal lng;
    private Boolean isDefault;

}
