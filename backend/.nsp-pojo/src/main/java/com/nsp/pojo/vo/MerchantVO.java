package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 商家/店铺信息响应（含距离，用于用户端的附近商家列表）
 */
@Data
public class MerchantVO {

    private Long id;
    private String name;
    private String phone;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String avatarUrl;
    private String status;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime businessHoursStart;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime businessHoursEnd;

    /** 配送半径（米） */
    private Integer deliveryRadius;

    /** 距离（米），仅用户端计算距离时填充 */
    private Integer distance;

}
