package com.nsp.server.service;

import com.nsp.pojo.dto.ApproveApplicationDTO;
import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.dto.RejectApplicationDTO;
import com.nsp.pojo.vo.AdminApplicationVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.PageVO;

/**
 * 入驻审核业务接口 — 申请列表、审核通过/拒绝、申请详情
 */
public interface ApplicationReviewService {

    /** 商家入驻申请列表（分页+筛选+品类+日期范围） */
    PageVO<AdminApplicationVO> listApplications(int page, int size, String keyword, String status,
                                               Long categoryId, String dateStart, String dateEnd);

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

    /** 批量通过（已审核的自动跳过，默认营业中+配送半径5000） */
    BatchResultVO batchApprove(java.util.List<Long> ids, Long reviewerId);

    /** 批量拒绝（统一拒绝原因，已审核的自动跳过） */
    BatchResultVO batchReject(java.util.List<Long> ids, Long reviewerId, String rejectReason);

}
