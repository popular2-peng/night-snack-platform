package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.Order;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单数据访问层
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
}
