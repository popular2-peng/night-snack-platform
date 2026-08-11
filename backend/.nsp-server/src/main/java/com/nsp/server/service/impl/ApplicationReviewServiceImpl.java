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
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.mapper.MerchantApplicationMapper;
import com.nsp.server.mapper.PlatformCategoryMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.UserMapper;
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

        List<AdminApplicationVO> list = result.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public AdminApplicationVO getApplicationDetail(Long id) {
        return toVO(findById(id));
    }

    /** 申请 → VO（补充申请人用户名/脱敏手机号/品类名/完整地址） */
    private AdminApplicationVO toVO(MerchantApplication a) {
        AdminApplicationVO vo = new AdminApplicationVO();
        BeanUtil.copyProperties(a, vo);

        User applicant = userMapper.selectById(a.getApplicantUserId());
        vo.setApplicantUsername(applicant != null ? applicant.getUsername() : "未知");
        vo.setPhone(applicant != null ? PhoneMaskUtil.mask(applicant.getPhone()) : "");

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

        // 2. 校验申请凭据
        if (app.getUsername() == null || app.getUsername().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.PARAM_APPLICATION_MISSING_USERNAME_MSG);
        }
        if (app.getPassword() == null || app.getPassword().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.PARAM_APPLICATION_MISSING_PASSWORD_MSG);
        }

        // 3. 创建商家记录（凭据来自商家注册时填写，密码已 BCrypt 加密）
        Merchant merchant = new Merchant();
        merchant.setUsername(app.getUsername());
        merchant.setPassword(app.getPassword());
        merchant.setName(app.getShopName());
        merchant.setPhone(applicant.getPhone());
        merchant.setLogoUrl(app.getLogoUrl());
        merchant.setCategoryId(app.getCategoryId());
        merchant.setDescription(app.getDescription());
        merchant.setIdCardNo(app.getIdCardNo());
        merchant.setStatus(dto.getInitialStatus() != null ? dto.getInitialStatus() : "OPEN");
        merchant.setDeliveryRadius(dto.getDeliveryRadius() != null ? dto.getDeliveryRadius() : 5000);
        merchant.setAvgRating(BigDecimal.valueOf(5.0));                // 新店默认 5.0 分
        merchant.setTotalOrders(0);
        // 注册时填写的地址 + 百度地理编码坐标（若有）
        merchant.setAddress(joinAddress(app));
        merchant.setLat(app.getLat());
        merchant.setLng(app.getLng());
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        merchantMapper.insert(merchant);

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

        // 2. 校验申请凭据
        if (app.getUsername() == null || app.getUsername().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.PARAM_APPLICATION_MISSING_USERNAME_MSG);
        }
        if (app.getPassword() == null || app.getPassword().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, MessageConst.PARAM_APPLICATION_MISSING_PASSWORD_MSG);
        }

        // 3. 创建商家记录（凭据来自商家注册时填写，密码已 BCrypt 加密）
        Merchant merchant = new Merchant();
        merchant.setUsername(app.getUsername());
        merchant.setPassword(app.getPassword());
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
