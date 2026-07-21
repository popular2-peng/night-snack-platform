package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细数据访问层
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
