package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家入驻申请列表项 — V2 含完整字段
 */
@Data
public class AdminApplicationVO {

    private Long id;
    private String shopName;
    private String applicantUsername;
    /** 手机号（脱敏） */
    private String phone;
    /** 店铺 LOGO */
    private String logoUrl;
    /** 主营品类名称 */
    private String categoryName;
    /** PENDING / APPROVED / REJECTED */
    private String status;
    /** 拒绝原因 */
    private String rejectReason;
    /** 联系人姓名（申请人，全中文可含间隔号·） */
    private String ownerName;
    /** 完整地址（省 + 市 + 区县 + 详细地址） */
    private String address;
    /** 纬度（百度地理编码） */
    private BigDecimal lat;
    /** 经度（百度地理编码） */
    private BigDecimal lng;

    /** 申请时间（ISO-8601 T 格式，见全局 Jackson 配置） */
    private LocalDateTime createdAt;

    /** 审核时间（未审核为 null） */
    private LocalDateTime reviewedAt;

}
