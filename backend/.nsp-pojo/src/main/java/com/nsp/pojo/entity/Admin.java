package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台管理员 — 对应 admin 表
 */
@Data
@TableName("admin")
public class Admin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录用，唯一） */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 手机号 */
    private String phone;

    /** 状态：ACTIVE / DISABLED */
    private String status;

    /** 头像 OSS URL */
    private String avatarUrl;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
