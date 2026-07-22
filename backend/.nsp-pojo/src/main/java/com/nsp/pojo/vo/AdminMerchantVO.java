package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台商家列表项
 */
@Data
public class AdminMerchantVO {

    private Long id;
    private String name;
    private String ownerName;
    private String phone;
    /** PENDING / OPEN / CLOSED / SUSPENDED */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

}
