package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家入驻申请 — 用户申请成为商家的审核记录
 */
@Data
@TableName("merchant_application")
public class MerchantApplication {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人用户 ID */
    private Long applicantUserId;

    /** 申请的店铺名称 */
    private String shopName;

    /** 商家登录用户名（注册时填写） */
    private String username;

    /** 商家登录密码（BCrypt，注册时填写） */
    private String password;

    /** 审核状态：PENDING / APPROVED / REJECTED */
    private String status;

    /** 审核人 ID（管理员） */
    private Long reviewerId;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // ==================== V2 新增字段 ====================

    /** 申请时上传的店铺 LOGO */
    private String logoUrl;

    /** 申请的主营品类 ID */
    private Long categoryId;

    /** 店铺简介 */
    private String description;

    /** 店主身份证号（加密） */
    private String idCardNo;

    /** 审核备注（通过/拒绝时填写） */
    private String reviewNote;

    /** 拒绝原因（展示给商家看） */
    private String rejectReason;

    // ==================== 入驻地址信息（注册时填写） ====================

    /** 联系人姓名（店主，中文） */
    private String ownerName;

    /** 所在省 */
    private String province;

    /** 所在市 */
    private String city;

    /** 所在区县 */
    private String district;

    /** 详细地址 */
    private String address;

    /** 纬度（百度地理编码结果） */
    private java.math.BigDecimal lat;

    /** 经度（百度地理编码结果） */
    private java.math.BigDecimal lng;

}
