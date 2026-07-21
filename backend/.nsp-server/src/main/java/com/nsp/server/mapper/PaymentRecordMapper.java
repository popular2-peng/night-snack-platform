package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录数据访问层
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}
