package com.nsp.server.service;

import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.vo.*;

/**
 * 管理员业务接口 — 商家审核、用户管理、商家管理、数据统计
 */
public interface AdminService {

    /** 首页统计 */
    AdminDashboardVO getDashboardStats();

    /** 商家入驻申请列表（分页） */
    PageVO<AdminApplicationVO> listApplications(int page, int size);

    /** 通过入驻申请 */
    void approveApplication(Long id, Long reviewerId);

    /** 拒绝入驻申请 */
    void rejectApplication(Long id, Long reviewerId, AuditDTO dto);

    /** 用户列表（分页+搜索） */
    PageVO<AdminUserVO> listUsers(int page, int size, String keyword);

    /** 封禁用户 */
    void disableUser(Long id);

    /** 解封用户 */
    void enableUser(Long id);

    /** 商家列表（分页+筛选+搜索） */
    PageVO<AdminMerchantVO> listMerchants(int page, int size, String keyword, String status);

    /** 封禁商家 */
    void suspendMerchant(Long id);

    /** 解封商家 */
    void activateMerchant(Long id);

}
