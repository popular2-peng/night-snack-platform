package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nsp.common.enums.UserRole;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 — 平台管理员 + 商家 + 普通用户共用
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

    /** 角色：USER / MERCHANT / ADMIN */
    private UserRole role;

    /** 状态：ACTIVE / DISABLED（管理员可封禁用户） */
    private String status;

    /** 头像 OSS URL */
    private String avatarUrl;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
