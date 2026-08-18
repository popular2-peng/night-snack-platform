package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 — 用户详情
 */
@Data
public class UserDetailVO {

    private Long id;
    /** 用户名（后端生成，展示用） */
    private String username;
    /** 手机号（脱敏） */
    private String phone;
    /** 头像 OSS URL */
    private String avatarUrl;
    /** ACTIVE / DISABLED */
    private String status;
    /** 历史总订单数（order 表实时统计） */
    private Integer totalOrders;
    /** 历史总消费金额（order 表实时统计） */
    private BigDecimal totalSpent;

    private LocalDateTime lastLoginAt;

    /** 封禁原因 */
    private String bannedReason;
    /** 封禁时间 */
    private LocalDateTime bannedAt;
    /** 解封时间 */
    private LocalDateTime unbannedAt;
    /** 管理员备注 */
    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
