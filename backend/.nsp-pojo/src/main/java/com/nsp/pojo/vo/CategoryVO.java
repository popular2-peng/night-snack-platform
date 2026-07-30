package com.nsp.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedAt;

}
