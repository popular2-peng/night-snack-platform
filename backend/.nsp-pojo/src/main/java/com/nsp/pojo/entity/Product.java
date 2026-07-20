package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品
 */
@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家 ID */
    private Long merchantId;

    /** 所属品类 ID */
    private Long categoryId;

    /** 商品名称 */
    private String name;

    /** 商品基础价格 */
    private BigDecimal price;

    /** 商品图片 OSS URL */
    private String imageUrl;

    /** 状态：上架 / 下架 */
    private String status;

    /** 商品描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
