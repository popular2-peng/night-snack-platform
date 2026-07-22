package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    /** 状态：ACTIVE / DISABLED（管理员可封禁） */
    private String status;

    /** 头像 OSS URL */
    private String avatarUrl;

    /** 注册时间 */
    private LocalDateTime createdAt;

}
