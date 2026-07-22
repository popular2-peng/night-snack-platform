package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台用户列表项
 */
@Data
public class AdminUserVO {

    private Long id;
    private String username;
    private String phone;
    private String role;
    /** ACTIVE / DISABLED */
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

}
