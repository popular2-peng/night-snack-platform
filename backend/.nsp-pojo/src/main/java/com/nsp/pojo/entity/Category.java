package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品品类 — 商家内部的商品分类（如"烧烤类""饮品类"）
 */
@Data
@TableName("product_category")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属商家 ID */
    private Long merchantId;

    /** 品类名称 */
    private String name;

    /** 排序（数字越小越靠前） */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createdAt;

}
