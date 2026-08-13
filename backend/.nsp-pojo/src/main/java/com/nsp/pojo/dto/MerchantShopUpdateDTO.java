package com.nsp.pojo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家更新店铺信息请求体 — 全字段可选（部分更新，仅更新非空字段）
 *
 * <p>商家身份由 JWT 解析（sub = merchant.id），无需传 merchantId。
 */
@Data
public class MerchantShopUpdateDTO {

    /** 店铺名称 */
    @Size(min = 2, max = 20, message = "店铺名称 2-20 字")
    private String name;

    /** 店铺 LOGO OSS URL（先走 /api/upload/sign 直传 OSS 后回传 URL） */
    @Size(max = 500, message = "LOGO URL 过长")
    private String logoUrl;

    /** 详细地址（修改后服务端调用百度地理编码重新解析坐标；解析失败保留原坐标并告警） */
    @Size(max = 200, message = "详细地址最多 200 字")
    private String address;

    /** 营业开始时间（HH:mm），与结束时间必须成对填写（服务端校验） */
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "营业开始时间格式应为 HH:mm")
    private String businessHoursStart;

    /** 营业结束时间（HH:mm，支持跨天，如 02:00 表示次日凌晨） */
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "营业结束时间格式应为 HH:mm")
    private String businessHoursEnd;

    /** 配送半径（米），默认 5000，允许 500-10000 */
    @Min(value = 500, message = "配送半径最小 500 米")
    @Max(value = 10000, message = "配送半径最大 10000 米")
    private Integer deliveryRadius;

    /** 店铺简介 */
    @Size(max = 200, message = "店铺简介最多 200 字")
    private String description;

    /** 店铺公告 */
    @Size(max = 200, message = "店铺公告最多 200 字")
    private String notice;

}
