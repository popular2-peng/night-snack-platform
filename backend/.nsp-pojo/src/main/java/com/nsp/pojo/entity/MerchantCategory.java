package com.nsp.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 商家-品类多对多关联
 *
 * <p>一个商家可属于多个平台品类（如"烧烤类" + "饮品类"）
 */
@Data
@TableName("merchant_category")
public class MerchantCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商家 ID */
    private Long merchantId;

    /** 平台品类 ID */
    private Long categoryId;

    /** 排序权重 */
    private Integer sortOrder;

}
