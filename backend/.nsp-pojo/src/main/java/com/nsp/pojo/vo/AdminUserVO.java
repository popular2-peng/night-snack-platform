package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台用户列表项 — 含完整字段
 */
@Data
public class AdminUserVO {

    private Long id;
    /** 用户名（后端生成，展示用） */
    private String username;
    /** 手机号（脱敏：138****0002） */
    private String phone;
    /** 头像 OSS URL */
    private String avatarUrl;
    /** ACTIVE / DISABLED */
    private String status;
    /** 历史总订单数（order 表实时统计） */
    private Integer totalOrders;
    /** 历史总消费金额（order 表实时统计） */
    private BigDecimal totalSpent;

    /** 最后登录时间（登录成功后实时更新） */
    private LocalDateTime lastLoginAt;

    /** 注册时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
