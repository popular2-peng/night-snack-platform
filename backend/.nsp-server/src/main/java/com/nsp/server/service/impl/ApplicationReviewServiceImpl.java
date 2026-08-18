package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.common.util.PhoneMaskUtil;
import com.nsp.pojo.dto.ApproveApplicationDTO;
import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.dto.RejectApplicationDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantApplication;
import com.nsp.pojo.entity.PlatformCategory;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.AdminApplicationVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.mapper.MerchantApplicationMapper;
import com.nsp.server.mapper.PlatformCategoryMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.UserMapper;
import com.nsp.server.cache.HomeMerchantListCache;
import com.nsp.server.service.ApplicationReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入驻审核业务实现
 */
@Service
@RequiredArgsConstructor
public class ApplicationReviewServiceImpl implements ApplicationReviewService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationReviewServiceImpl.class);

    private final MerchantApplicationMapper merchantApplicationMapper;
    private final HomeMerchantListCache homeMerchantListCache;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final PlatformCategoryMapper platformCategoryMapper;

    @Override
    public PageVO<AdminApplicationVO> listApplications(int page, int size, String keyword, String status,
                                                       Long categoryId, String dateStart, String dateEnd) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(MerchantApplication::getShopName, keyword)
                            .or().like(MerchantApplication::getApplicantUserId, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(MerchantApplication::getStatus, status.toUpperCase());
        }
        // 品类筛选
        if (categoryId != null) {
            wrapper.eq(MerchantApplication::getCategoryId, categoryId);
        }
        // 申请时间范围
        if (dateStart != null && !dateStart.isBlank()) {
            wrapper.ge(MerchantApplication::getCreatedAt, LocalDateTime.parse(dateStart + "T00:00:00"));
        }
        if (dateEnd != null && !dateEnd.isBlank()) {
            wrapper.le(MerchantApplication::getCreatedAt, LocalDateTime.parse(dateEnd + "T23:59:59"));
        }
        // 最新申请在前
        wrapper.orderByDesc(MerchantApplication::getId);

        Page<MerchantApplication> result = merchantApplicationMapper.selectPage(
                new Page<>(page, size), wrapper);

        List<AdminApplicationVO> list = result.getRecords().stream().map(a -> toVO(a, true)).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public AdminApplicationVO getApplicationDetail(Long id) {
        return toVO(findById(id), false);
    }

    /** 申请 → VO（补充申请人用户名/手机号/品类名/完整地址）；maskPhone=true 列表脱敏，false 详情明文 */
    private AdminApplicationVO toVO(MerchantApplication a, boolean maskPhone) {
        AdminApplicationVO vo = new AdminApplicationVO();
        BeanUtil.copyProperties(a, vo);

        User applicant = userMapper.selectById(a.getApplicantUserId());
        vo.setApplicantUsername(applicant != null ? applicant.getUsername() : "未知");
        vo.setPhone(applicant != null
                ? (maskPhone ? PhoneMaskUtil.mask(applicant.getPhone()) : applicant.getPhone())
                : "");

        if (a.getCategoryId() != null) {
            PlatformCategory c = platformCategoryMapper.selectById(a.getCategoryId());
            vo.setCategoryName(c != null ? c.getName() : "");
        }

        // 完整地址：省 + 市 + 区县 + 详细地址
        StringBuilder addr = new StringBuilder();
        if (a.getProvince() != null) addr.append(a.getProvince());
        if (a.getCity() != null) addr.append(a.getCity());
        if (a.getDistrict() != null) addr.append(a.getDistrict());
        if (a.getAddress() != null) addr.append(a.getAddress());
        vo.setAddress(addr.length() == 0 ? null : addr.toString());

        return vo;
    }

    @Override
    @Transactional
    public void approveApplicationV2(Long id, Long reviewerId, ApproveApplicationDTO dto) {
        MerchantApplication app = findById(id);

        // 1. 查询申请人信息
        User applicant = userMapper.selectById(app.getApplicantUserId());
        if (applicant == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.APPLICANT_NOT_FOUND_MSG);
        }

        // 2. 校验申请凭据（密码已 BCrypt 加密，审核通过后直接复用；账号即手机号，无 username）
        if (app.getPassword() == null || app.getPassword().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.PARAM_APPLICATION_MISSING_PASSWORD_MSG);
        }

        // 3. 创建商家记录（凭据来自商家注册时填写，密码已 BCrypt 加密）
        Merchant merchant = new Merchant();
        merchant.setPassword(app.getPassword());
        merchant.setOwnerName(app.getOwnerName());
        merchant.setName(app.getShopName());
        merchant.setPhone(applicant.getPhone());
        merchant.setLogoUrl(app.getLogoUrl());
        merchant.setCategoryId(app.getCategoryId());
        merchant.setDescription(app.getDescription());
        merchant.setIdCardNo(app.getIdCardNo());
        merchant.setStatus(dto.getInitialStatus() != null ? dto.getInitialStatus() : "OPEN");
        // 配送半径由商家审核通过后自行设置，此处给默认值 5000 米
        merchant.setDeliveryRadius(5000);
        merchant.setAvgRating(BigDecimal.valueOf(5.0));                // 新店默认 5.0 分
        merchant.setTotalOrders(0);
        // 注册时填写的地址 + 百度地理编码坐标（若有）
        merchant.setAddress(joinAddress(app));
        merchant.setLat(app.getLat());
        merchant.setLng(app.getLng());
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.insert(merchant);
        // 新商家入驻会影响首页商家列表，清除缓存
        homeMerchantListCache.evict();

        // 4. 更新申请状态
        app.setStatus("APPROVED");
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        if (dto.getReviewNote() != null) {
            app.setReviewNote(dto.getReviewNote());
        }
        merchantApplicationMapper.updateById(app);

        log.info("商家入驻申请通过(V2): appId={}, reviewerId={}, merchantId={}, merchantName={}, status={}",
                id, reviewerId, merchant.getId(), merchant.getName(), merchant.getStatus());
    }

    @Override
    @Transactional
    public void rejectApplicationV2(Long id, Long reviewerId, RejectApplicationDTO dto) {
        MerchantApplication app = findById(id);

        app.setStatus("REJECTED");
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        app.setRejectReason(dto.getRejectReason());
        merchantApplicationMapper.updateById(app);

        log.info("商家入驻申请被拒绝(V2): appId={}, reason={}", id, dto.getRejectReason());
    }

    @Override
    @Transactional
    public void approveApplication(Long id, Long reviewerId) {
        MerchantApplication app = findById(id);

        // 1. 查询申请人信息
        User applicant = userMapper.selectById(app.getApplicantUserId());
        if (applicant == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, MessageConst.APPLICANT_NOT_FOUND_MSG);
        }

        // 2. 校验申请凭据（密码已 BCrypt 加密，审核通过后直接复用；账号即手机号，无 username）
        if (app.getPassword() == null || app.getPassword().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.PARAM_APPLICATION_MISSING_PASSWORD_MSG);
        }

        // 3. 创建商家记录（凭据来自商家注册时填写，密码已 BCrypt 加密）
        Merchant merchant = new Merchant();
        merchant.setPassword(app.getPassword());
        merchant.setOwnerName(app.getOwnerName());
        merchant.setName(app.getShopName());
        merchant.setPhone(applicant.getPhone());
        merchant.setLogoUrl(app.getLogoUrl());
        merchant.setCategoryId(app.getCategoryId());
        merchant.setDescription(app.getDescription());
        merchant.setIdCardNo(app.getIdCardNo());
        merchant.setStatus("OPEN");
        merchant.setDeliveryRadius(5000);
        merchant.setAvgRating(BigDecimal.valueOf(5.0));
        merchant.setTotalOrders(0);
        // 注册时填写的地址 + 百度地理编码坐标（若有）
        merchant.setAddress(joinAddress(app));
        merchant.setLat(app.getLat());
        merchant.setLng(app.getLng());
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.insert(merchant);
        // 新商家入驻会影响首页商家列表，清除缓存
        homeMerchantListCache.evict();

        // 4. 更新申请状态
        app.setStatus("APPROVED");
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        merchantApplicationMapper.updateById(app);

        log.info("商家入驻申请通过(V1): appId={}, reviewerId={}, merchantId={}, merchantName={}",
                id, reviewerId, merchant.getId(), merchant.getName());
    }

    @Override
    @Transactional
    public void rejectApplication(Long id, Long reviewerId, AuditDTO dto) {
        MerchantApplication app = findById(id);

        app.setStatus("REJECTED");
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        merchantApplicationMapper.updateById(app);

        log.info("商家入驻申请被拒绝: appId={}, reason={}", id, dto.getReason());
    }

    @Override
    @Transactional
    public BatchResultVO batchApprove(List<Long> ids, Long reviewerId) {
        BatchResultVO result = new BatchResultVO();
        int audited = 0, missing = 0;
        for (Long id : ids) {
            MerchantApplication app = merchantApplicationMapper.selectById(id);
            if (app == null) { missing++; continue; }
            // 已审核（通过/拒绝）的跳过
            if (!"PENDING".equals(app.getStatus())) { audited++; continue; }

            // 批量通过使用默认配置：营业中 + 配送半径 5000
            approveApplicationV2(id, reviewerId, new ApproveApplicationDTO());
            result.setSuccessCount(result.getSuccessCount() + 1);
        }
        result.setSkippedCount(audited + missing);
        if (audited > 0) result.addSkipped(audited + " 条申请已审核");
        if (missing > 0) result.addSkipped(missing + " 条申请不存在");
        log.info("批量通过入驻申请: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    @Override
    @Transactional
    public BatchResultVO batchReject(List<Long> ids, Long reviewerId, String rejectReason) {
        BatchResultVO result = new BatchResultVO();
        int audited = 0, missing = 0;
        for (Long id : ids) {
            MerchantApplication app = merchantApplicationMapper.selectById(id);
            if (app == null) { missing++; continue; }
            if (!"PENDING".equals(app.getStatus())) { audited++; continue; }

            RejectApplicationDTO dto = new RejectApplicationDTO();
            dto.setRejectReason(rejectReason);
            rejectApplicationV2(id, reviewerId, dto);
            result.setSuccessCount(result.getSuccessCount() + 1);
        }
        result.setSkippedCount(audited + missing);
        if (audited > 0) result.addSkipped(audited + " 条申请已审核");
        if (missing > 0) result.addSkipped(missing + " 条申请不存在");
        log.info("批量拒绝入驻申请: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    private MerchantApplication findById(Long id) {
        MerchantApplication app = merchantApplicationMapper.selectById(id);
        if (app == null) throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, MessageConst.APPLICATION_NOT_FOUND_MSG);
        return app;
    }

    /** 拼接申请地址：省 + 市 + 区县 + 详细地址 */
    private String joinAddress(MerchantApplication app) {
        StringBuilder sb = new StringBuilder();
        if (app.getProvince() != null) sb.append(app.getProvince());
        if (app.getCity() != null) sb.append(app.getCity());
        if (app.getDistrict() != null) sb.append(app.getDistrict());
        if (app.getAddress() != null) sb.append(app.getAddress());
        return sb.length() == 0 ? null : sb.toString();
    }

}
