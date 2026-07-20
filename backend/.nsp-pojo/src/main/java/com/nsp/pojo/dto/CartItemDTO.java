package com.nsp.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车添加/修改商品请求参数
 */
@Data
public class CartItemDTO {

    @NotNull(message = "规格 ID 不能为空")
    private Long specId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少为 1")
    private Integer quantity;

}
