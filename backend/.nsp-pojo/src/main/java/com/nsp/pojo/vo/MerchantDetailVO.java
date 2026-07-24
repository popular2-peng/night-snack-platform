package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 商家详情 — 含全部字段
 */
@Data
@Builder
public class MerchantDetailVO {

    private Long id;
    private String username;
    private String name;
    /** 手机号（脱敏） */
    private String phone;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private String logoUrl;
    /** 主营品类名称 */
    private String categoryName;
    private String description;
    /** 身份证号（脱敏） */
    private String idCardNo;
    /** 店铺评分 */
    private BigDecimal avgRating;
    /** 历史总订单数 */
    private Integer totalOrders;
    /** 今日订单数 */
    private Integer todayOrders;
    /** 今日营收 */
    private BigDecimal todayRevenue;
    /** 营业状态 */
    private String status;
    /** 封禁原因 */
    private String bannedReason;
    /** 封禁时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime bannedAt;
    /** 店铺公告 */
    private String notice;

    // 营业时间
    @JsonFormat(pattern = "HH:mm")
    private LocalTime businessHoursStart;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime businessHoursEnd;

    private Integer deliveryRadius;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

}
