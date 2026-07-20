package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户地址数据访问层
 */
@Mapper
public interface AddressMapper extends BaseMapper<UserAddress> {
}
