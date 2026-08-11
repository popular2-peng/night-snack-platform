package com.nsp.server.service;

import com.nsp.pojo.dto.BanMerchantDTO;
import com.nsp.pojo.vo.AdminMerchantVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.MerchantDetailVO;
import com.nsp.pojo.vo.MerchantStatsVO;
import com.nsp.pojo.vo.PageVO;

import java.util.List;
import java.util.Map;

/**
 * 商家管理业务接口 — 管理员对商家的列表查询、CRUD、封禁、操作日志
 */
public interface MerchantAdminService {

    /** 商家列表（分页+筛选+搜索+品类+日期范围） */
    PageVO<AdminMerchantVO> listMerchants(int page, int size, String keyword, String status,
                                         Long categoryId, String dateStart, String dateEnd);

    /** 封禁商家（简易版，仅设 SUSPENDED 状态） */
    void suspendMerchant(Long id);

    /** 解封商家（简易版，恢复 OPEN） */
    void activateMerchant(Long id);

    /** 商家统计概览（总数/营业中/待审核/已封禁/打烊） */
    MerchantStatsVO getStats();

    /** 商家详情（含完整字段） */
    MerchantDetailVO getDetail(Long id);

    /** 暂停营业（status → CLOSED） */
    void pause(Long id, Long operatorId);

    /** 恢复营业（status → OPEN） */
    void resume(Long id, Long operatorId);

    /** 封禁（body 含 reason，必填） */
    void ban(Long id, Long operatorId, BanMerchantDTO dto);

    /** 解封 */
    void unban(Long id, Long operatorId);

    /** 软删（deleted=1） */
    void softDelete(Long id, Long operatorId);

    /** 批量删除商家（级联清理订单/商品等，未封禁/不存在的跳过） */
    BatchResultVO batchDelete(java.util.List<Long> ids);

    /** 操作日志列表（简易版，返回 Map 列表） */
    List<Map<String, Object>> getOperationLogs(Long id);

}
