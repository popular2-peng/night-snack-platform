package com.nsp.server.service;

import com.nsp.pojo.dto.BanUserDTO;
import com.nsp.pojo.vo.AdminUserVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.pojo.vo.UserDetailVO;

import java.util.List;
import java.util.Map;

/**
 * 用户管理业务接口 — 管理员对顾客 user 表的管理
 */
public interface UserManagementService {

    /**
     * 用户列表（分页+筛选+搜索）
     *
     * @param keyword   搜索：用户名/手机号/昵称
     * @param status    状态筛选：ACTIVE/DISABLED
     * @param dateStart 注册时间范围-起始
     * @param dateEnd   注册时间范围-截止
     */
    PageVO<AdminUserVO> listUsers(int page, int size, String keyword,
                                   String status, String dateStart, String dateEnd);

    /** 用户详情（含完整字段：地址列表、操作日志） */
    UserDetailVO getUserDetail(Long id);

    /**
     * 封禁用户
     *
     * @param id         用户 ID
     * @param operatorId 操作管理员 ID
     * @param dto        封禁原因 + 时长
     */
    void banUser(Long id, Long operatorId, BanUserDTO dto);

    /**
     * 解封用户
     *
     * @param id         用户 ID
     * @param operatorId 操作管理员 ID
     */
    void unbanUser(Long id, Long operatorId);

    /** 重置用户密码（返回新密码） */
    String resetPassword(Long id, Long operatorId);

    /** 批量封禁（已封禁/已删除的跳过） */
    BatchResultVO batchBan(List<Long> ids, Long operatorId);

    /** 批量启用（未封禁/已删除的跳过） */
    BatchResultVO batchUnban(List<Long> ids, Long operatorId);

    /** 批量删除（软删，启用账号不可删除：ACTIVE 跳过） */
    BatchResultVO batchDelete(List<Long> ids, Long operatorId);

    /** 用户操作日志列表 */
    List<Map<String, Object>> getOperationLogs(Long id);

}
