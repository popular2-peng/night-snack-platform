package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.UploadedFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 上传文件数据访问层
 */
@Mapper
public interface UploadedFileMapper extends BaseMapper<UploadedFile> {
}
