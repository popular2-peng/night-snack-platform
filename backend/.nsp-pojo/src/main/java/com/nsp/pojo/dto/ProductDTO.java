package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增/编辑请求参数
 */
@Data
public class ProductDTO {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    @NotNull(message = "品类 ID 不能为空")
    private Long categoryId;

    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;

    /** 商品图片 OSS URL（先上传 OSS 再填此字段） */
    private String imageUrl;

    /** 商品描述 */
    private String description;

}
