package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品规格（如"大杯 +3元""加珍珠 +1元"）
 */
@Data
@TableName("product_spec")
public class ProductSpec {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商品 ID */
    private Long productId;

    /** 规格名称（如"大杯""中杯""加珍珠"） */
    private String name;

    /** 价格增量（可正可负，加到 product.price 上） */
    private BigDecimal priceDelta;

    /** 库存数量（Redis 扣减时以此为上限） */
    private Integer stock;

}
