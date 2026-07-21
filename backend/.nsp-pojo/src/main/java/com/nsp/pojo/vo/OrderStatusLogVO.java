package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单状态变更记录响应
 */
@Data
public class OrderStatusLogVO {

    private String fromStatus;
    private String toStatus;
    private String operator;
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

}
