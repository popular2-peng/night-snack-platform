package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.MerchantApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家入驻申请数据访问层
 */
@Mapper
public interface MerchantApplicationMapper extends BaseMapper<MerchantApplication> {
}
