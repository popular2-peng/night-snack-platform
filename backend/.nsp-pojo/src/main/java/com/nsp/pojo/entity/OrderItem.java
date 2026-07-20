package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细 — 订单中的单个商品（含规格），价格以下单时快照为准
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单 ID */
    private Long orderId;

    /** 商品 ID */
    private Long productId;

    /** 规格 ID */
    private Long specId;

    /** 商品名称快照（下单时的名称，后续商品改名不影响历史订单） */
    private String nameSnapshot;

    /** 价格快照（下单时的实际单价） */
    private BigDecimal priceSnapshot;

    /** 购买数量 */
    private Integer quantity;

}
