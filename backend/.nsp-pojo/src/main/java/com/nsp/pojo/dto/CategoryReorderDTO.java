package com.nsp.pojo.dto;

import lombok.Data;

/**
 * 品类批量排序请求体
 */
@Data
public class CategoryReorderDTO {

    /** 品类 ID */
    private Long id;

    /** 新的排序权重 */
    private Integer sortOrder;

}
