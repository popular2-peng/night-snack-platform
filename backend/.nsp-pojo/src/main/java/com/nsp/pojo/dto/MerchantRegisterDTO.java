package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家入驻申请（注册）请求体
 *
 * <p>商家注册 = 提交入驻申请，审核通过后凭据直接用于创建商家账号
 */
@Data
public class MerchantRegisterDTO {

    /** 商家登录账号：4-20 位，字母开头，字母/数字/下划线 */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{3,19}$", message = "账号格式不正确")
    private String username;

    /** 登录密码（明文，后端 BCrypt 加密存储） */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 店铺名称 */
    @NotBlank(message = "店铺名称不能为空")
    @Size(min = 2, max = 20, message = "店铺名称 2-20 字")
    private String shopName;

    /** 主营品类 ID（平台 category 表） */
    @NotNull(message = "请选择主营品类")
    private Long categoryId;

    /** 申请理由（可选） */
    @Size(max = 500, message = "申请理由最多 500 字")
    private String reason;

    /** 店铺简介（可选） */
    @Size(max = 200, message = "店铺简介最多 200 字")
    private String description;

    /** 店铺 LOGO OSS URL（可选） */
    private String logoUrl;

    /** 联系人姓名（申请人/店主，必须为中文字符） */
    @NotBlank(message = "联系人姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "联系人姓名需为 2-10 个中文字符")
    private String ownerName;

    /** 所在省 */
    @NotBlank(message = "请选择省份")
    private String province;

    /** 所在市 */
    @NotBlank(message = "请选择城市")
    private String city;

    /** 所在区县 */
    @NotBlank(message = "请选择区县")
    private String district;

    /** 详细地址（后端调百度地图地理编码获取坐标） */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 200, message = "详细地址最多 200 字")
    private String address;

}
