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

    /** 申请理由 */
    private String reason;

    /** 审核状态：PENDING / APPROVED / REJECTED */
    private String status;

    /** 审核人 ID（管理员） */
    private Long reviewerId;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
