package com.nsp.pojo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求参数
 *
 * <p>手机号 + 密码（用户/商家登录）或 用户名 + 密码（管理员登录），二选一
 */
@Data
public class LoginDTO {

    /** 用户名（管理员登录用） */
    private String username;

    /** 手机号（用户/商家登录用） */
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @AssertTrue(message = "请输入账号或手机号")
    public boolean isCredentialPresent() {
        return (username != null && !username.isBlank())
                || (phone != null && !phone.isBlank());
    }

}
