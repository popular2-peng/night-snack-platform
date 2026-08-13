package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 注册请求参数 — 无需填写用户名，注册时由系统生成「普通用户 + 随机串」
 */
@Data
public class RegisterDTO {

    /** 用户名（可选）— 注册时系统自动生成，前端无需传入 */
    private String username;

    /** 密码：6-20 位，必须同时包含字母和数字（与三端前端校验规则一致） */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*()_+]{6,20}$",
            message = "密码6-20位，需包含字母和数字")
    private String password;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

}
