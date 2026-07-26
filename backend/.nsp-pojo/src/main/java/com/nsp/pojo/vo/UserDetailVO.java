package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理后台 — 用户详情
 */
@Data
public class UserDetailVO {

    private Long id;
    private String username;
    private String nickname;
    /** 手机号（脱敏） */
    private String phone;
    /** 头像 OSS URL */
    private String avatarUrl;
    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;
    /** 生日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;
    /** ACTIVE / DISABLED */
    private String status;
    /** 历史总订单数 */
    private Integer totalOrders;
    /** 历史总消费金额 */
    private BigDecimal totalSpent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime lastLoginAt;

    /** 封禁原因 */
    private String bannedReason;
    /** 封禁时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime bannedAt;
    /** 解封时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime unbannedAt;
    /** 管理员备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedAt;

}
