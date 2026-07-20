package com.nsp.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情响应（含规格列表）
 */
@Data
public class ProductVO {

    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private String status;
    private String description;

    /** 所属品类 ID */
    private Long categoryId;

    /** 所属品类名称 */
    private String categoryName;

    /** 商品规格列表 */
    private List<SpecVO> specs;

}
