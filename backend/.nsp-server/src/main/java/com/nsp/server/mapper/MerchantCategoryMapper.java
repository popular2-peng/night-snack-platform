package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.MerchantCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家-品类多对多关联数据访问层
 */
@Mapper
public interface MerchantCategoryMapper extends BaseMapper<MerchantCategory> {
}
