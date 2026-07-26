package com.nsp.server.service;

import com.nsp.pojo.vo.AdminUserVO;
import com.nsp.pojo.vo.PageVO;

/**
 * 用户管理业务接口 — 管理员对顾客 user 表的管理
 */
public interface UserManagementService {

    /** 用户列表（分页+搜索） */
    PageVO<AdminUserVO> listUsers(int page, int size, String keyword);

    /** 封禁用户 */
    void disableUser(Long id);

    /** 解封用户 */
    void enableUser(Long id);

}
