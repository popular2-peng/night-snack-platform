package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 品类数据访问层
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
