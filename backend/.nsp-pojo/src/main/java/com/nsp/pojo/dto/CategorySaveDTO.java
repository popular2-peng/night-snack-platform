package com.nsp.pojo.dto;

import lombok.Data;

/**
 * 新增/修改品类请求体
 */
@Data
public class CategorySaveDTO {

    /** 品类名称（2-10 字） */
    private String name;

    /** 图标（Emoji 或 OSS URL） */
    private String icon;

    /** 图标类型：EMOJI / OSS */
    private String iconType;

    /** 排序权重（值越小越靠前，不传则取最大值+1） */
    private Integer sortOrder;

    /** 状态：ENABLED / DISABLED */
    private String status;

    /** 备注 */
    private String remark;

}
