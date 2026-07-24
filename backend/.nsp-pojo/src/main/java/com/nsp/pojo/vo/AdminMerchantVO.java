package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台商家列表项 — V2 含完整字段
 */
@Data
public class AdminMerchantVO {

    private Long id;
    private String name;
    private String ownerName;
    /** 手机号（脱敏：138****0002） */
    private String phone;
    /** 店铺 LOGO */
    private String logoUrl;
    /** 主营品类名称 */
    private String categoryName;
    /** 今日订单数 */
    private Integer todayOrders;
    /** 今日营收 */
    private BigDecimal todayRevenue;
    /** 店铺评分 */
    private BigDecimal avgRating;
    /** PENDING / OPEN / CLOSED / SUSPENDED */
    private String status;
    /** 最后登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

}
