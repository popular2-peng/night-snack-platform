package com.nsp.server.service;

import com.nsp.pojo.dto.ApproveApplicationDTO;
import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.dto.RejectApplicationDTO;
import com.nsp.pojo.vo.*;

/**
 * 管理员业务接口 — 商家审核、用户管理、商家管理、数据统计
 */
public interface AdminService {

    /** 首页核心统计 */
    AdminDashboardVO getStats();

    /** 订单状态分布（饼图/柱状图数据） */
    java.util.List<OrderStatusStatVO> getOrderStatusDistribution();

    /** 最新订单列表 */
    java.util.List<AdminRecentOrderVO> getRecentOrders(int limit);

    /** 商家入驻申请列表（分页+筛选） */
    PageVO<AdminApplicationVO> listApplications(int page, int size, String keyword, String status);

    /** 入驻申请详情 */
    AdminApplicationVO getApplicationDetail(Long id);

    /** 通过入驻申请（V2，含初始营业状态/配送半径/审核备注） */
    void approveApplicationV2(Long id, Long reviewerId, ApproveApplicationDTO dto);

    /** 拒绝入驻申请（V2，拒绝原因必填） */
    void rejectApplicationV2(Long id, Long reviewerId, RejectApplicationDTO dto);

    /** 通过入驻申请（旧签名，兼容） */
    void approveApplication(Long id, Long reviewerId);

    /** 拒绝入驻申请（旧签名，兼容） */
    void rejectApplication(Long id, Long reviewerId, AuditDTO dto);

    /** 用户列表（分页+搜索） */
    PageVO<AdminUserVO> listUsers(int page, int size, String keyword);

    /** 封禁用户 */
    void disableUser(Long id);

    /** 解封用户 */
    void enableUser(Long id);

    /** 商家列表（分页+筛选+搜索）— V2 含新字段 */
    PageVO<AdminMerchantVO> listMerchants(int page, int size, String keyword, String status);

    /** 封禁商家 */
    void suspendMerchant(Long id);

    /** 解封商家 */
    void activateMerchant(Long id);

}
