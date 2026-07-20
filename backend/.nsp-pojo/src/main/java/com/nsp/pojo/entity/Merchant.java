package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nsp.common.enums.MerchantStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 商家/店铺
 */
@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 店铺名称 */
    private String name;

    /** 店主的用户 ID（关联 user 表，role=MERCHANT） */
    private Long ownerUserId;

    /** 店铺联系电话 */
    private String phone;

    /** 店铺地址 */
    private String address;

    /** 纬度 */
    private BigDecimal lat;

    /** 经度 */
    private BigDecimal lng;

    /** 店铺头像/Logo OSS URL */
    private String avatarUrl;

    /** 营业状态 */
    private MerchantStatus status;

    /** 营业开始时间（如 18:00） */
    private LocalTime businessHoursStart;

    /** 营业结束时间（如 02:00 次日） */
    private LocalTime businessHoursEnd;

    /** 配送半径（米） */
    private Integer deliveryRadius;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
