package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单状态日志数据访问层
 */
@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}
