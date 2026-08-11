package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 物理删除（绕过 MyBatis-Plus 逻辑删除拦截：管理端删除用户需级联清理关联数据后真删）
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

}

