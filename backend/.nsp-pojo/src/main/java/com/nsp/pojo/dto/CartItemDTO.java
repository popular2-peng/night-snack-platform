package com.nsp.pojo.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车添加/修改商品请求参数
 *
 * <p>添加：`productId` 必填，`specId` 选填 —— 有规格商品同时传 specId，无规格商品只传 productId；
 * 更新数量：传 `cartKey`（购物车条目键：有规格=`{specId}`，无规格=`p{productId}`）+ `quantity`
 */
@Data
public class CartItemDTO {

    /** 商品 ID（添加场景必填，先按商品校验归属，再看有无规格；更新场景不传） */
    private Long productId;

    /** 规格 ID（选填）：有规格商品必须传（按规格加价/扣库存），无规格商品不传（按商品基础价） */
    private Long specId;

    /** 购物车条目键（更新数量时用：有规格=`{specId}`，无规格=`p{productId}`） */
    private String cartKey;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量最少为 1")
    private Integer quantity;

    @AssertTrue(message = "添加购物车：商品 ID 不能为空；更新数量：cartKey 不能为空")
    public boolean isSkuPresent() {
        // 更新数量场景传 cartKey，不要求 productId
        if (cartKey != null && !cartKey.isBlank()) return true;
        return productId != null;
    }

}
