package com.nsp.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台品类出参
 */
@Data
public class CategoryVO {

    private Long id;
    private String name;
    private String icon;
    private String iconType;
    private Integer sortOrder;
    private String status;
    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
