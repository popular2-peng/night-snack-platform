package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 普通用户（顾客） — 对应 user 表
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录用，唯一） */
    private String username;

    /** 密码（BCrypt 加密存储） */
    private String password;

    /** 手机号 */
    private String phone;

    /** 头像 OSS URL */
    private String avatarUrl;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 生日 */
    private LocalDate birthday;

    /** 状态：ACTIVE / DISABLED */
    private String status;

    /** 历史总订单数（缓存） */
    private Integer totalOrders;

    /** 历史总消费金额（缓存） */
    private BigDecimal totalSpent;

    /** 最后登录时间 */
    private LocalDateTime lastLoginAt;

    /** 封禁原因 */
    private String bannedReason;

    /** 封禁时间 */
    private LocalDateTime bannedAt;

    /** 封禁操作人（关联 admin.id） */
    private Long bannedBy;

    /** 解封时间 */
    private LocalDateTime unbannedAt;

    /** 解封操作人（关联 admin.id） */
    private Long unbannedBy;

    /** 管理员备注 */
    private String remark;

    /** 软删标记：0-否 1-是 */
    private Integer deleted;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
