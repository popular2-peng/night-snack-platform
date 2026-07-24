package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

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
    private String reason;
    /** 店铺 LOGO */
    private String logoUrl;
    /** 主营品类名称 */
    private String categoryName;
    /** PENDING / APPROVED / REJECTED */
    private String status;
    /** 拒绝原因 */
    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

}
