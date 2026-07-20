package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商家数据访问层
 */
@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {
}
