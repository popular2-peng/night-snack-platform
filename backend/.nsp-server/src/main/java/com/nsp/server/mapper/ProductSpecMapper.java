package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.ProductSpec;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品规格数据访问层
 */
@Mapper
public interface ProductSpecMapper extends BaseMapper<ProductSpec> {
}
