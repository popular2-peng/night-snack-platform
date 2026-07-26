package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 商家/店铺（含登录凭据，自包含）
 */
@Data
@TableName("merchant")
public class Merchant {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家登录用户名（唯一） */
    private String username;

    /** 登录密码（BCrypt 加密） */
    private String password;

    /** 店铺名称 */
    private String name;

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

    /** 营业状态：PENDING / OPEN / CLOSED / SUSPENDED */
    private String status;

    /** 营业开始时间（如 18:00） */
    private LocalTime businessHoursStart;

    /** 营业结束时间（如 02:00 次日） */
    private LocalTime businessHoursEnd;

    /** 配送半径（米） */
    private Integer deliveryRadius;

    /** 入驻时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    // ==================== V2 新增字段 ====================

    /** 店铺 LOGO/头像 OSS URL */
    private String logoUrl;

    /** 主营品类 ID（关联 category 表） */
    private Long categoryId;

    /** 店铺简介/招牌描述 */
    private String description;

    /** 店主身份证号（加密存储） */
    private String idCardNo;

    /** 店铺平均评分 */
    private BigDecimal avgRating;

    /** 历史总订单数 */
    private Integer totalOrders;

    /** 封禁原因（管理员填写） */
    private String bannedReason;

    /** 封禁时间 */
    private LocalDateTime bannedAt;

    /** 封禁操作人（关联 user.id） */
    private Long bannedBy;

    /** 商家最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 店铺公告/营业备注 */
    private String notice;

}
