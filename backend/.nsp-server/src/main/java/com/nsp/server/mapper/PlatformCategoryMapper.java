package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.PlatformCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台品类数据访问层 — 对应 category 表
 */
@Mapper
public interface PlatformCategoryMapper extends BaseMapper<PlatformCategory> {
}
