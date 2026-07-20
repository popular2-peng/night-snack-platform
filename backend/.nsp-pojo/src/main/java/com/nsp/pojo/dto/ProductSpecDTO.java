package com.nsp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品规格新增/编辑请求参数
 */
@Data
public class ProductSpecDTO {

    @NotBlank(message = "规格名称不能为空")
    private String name;

    /** 价格增量，默认 0（可与基础价格相同） */
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @NotNull(message = "库存数量不能为空")
    private Integer stock;

}
