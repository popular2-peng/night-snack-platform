package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.exception.BizException;
import com.nsp.common.util.PhoneMaskUtil;
import com.nsp.pojo.dto.ApproveApplicationDTO;
import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.dto.RejectApplicationDTO;
import com.nsp.pojo.entity.Category;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantApplication;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.AdminApplicationVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.mapper.CategoryMapper;
import com.nsp.server.mapper.MerchantApplicationMapper;
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
    private final CategoryMapper categoryMapper;

    @Override
    public PageVO<AdminApplicationVO> listApplications(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(MerchantApplication::getShopName, keyword)
                            .or().like(MerchantApplication::getApplicantUserId, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(MerchantApplication::getStatus, status.toUpperCase());
        }
        wrapper.orderByAsc(MerchantApplication::getCreatedAt);

        Page<MerchantApplication> result = merchantApplicationMapper.selectPage(
                new Page<>(page, size), wrapper);

        List<AdminApplicationVO> list = result.getRecords().stream().map(a -> {
            AdminApplicationVO vo = new AdminApplicationVO();
            BeanUtil.copyProperties(a, vo);

            User applicant = userMapper.selectById(a.getApplicantUserId());
            vo.setApplicantUsername(applicant != null ? applicant.getUsername() : "未知");
            vo.setPhone(applicant != null ? PhoneMaskUtil.mask(applicant.getPhone()) : "");

            if (a.getCategoryId() != null) {
                Category c = categoryMapper.selectById(a.getCategoryId());
                vo.setCategoryName(c != null ? c.getName() : "");
            }

            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public AdminApplicationVO getApplicationDetail(Long id) {
        MerchantApplication a = findById(id);
        AdminApplicationVO vo = new AdminApplicationVO();
        BeanUtil.copyProperties(a, vo);

        User applicant = userMapper.selectById(a.getApplicantUserId());
        vo.setApplicantUsername(applicant != null ? applicant.getUsername() : "未知");
        vo.setPhone(applicant != null ? PhoneMaskUtil.mask(applicant.getPhone()) : "");

        if (a.getCategoryId() != null) {
            Category c = categoryMapper.selectById(a.getCategoryId());
            vo.setCategoryName(c != null ? c.getName() : "");
        }

        return vo;
    }

    @Override
    @Transactional
    public void approveApplicationV2(Long id, Long reviewerId, ApproveApplicationDTO dto) {
        MerchantApplication app = findById(id);

        // 1. 查询申请人信息
        User applicant = userMapper.selectById(app.getApplicantUserId());
        if (applicant == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, "申请人不存在");
        }

        // 2. 校验申请凭据
        if (app.getUsername() == null || app.getUsername().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "申请缺少商家登录用户名");
        }
        if (app.getPassword() == null || app.getPassword().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "申请缺少商家登录密码");
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
            throw new BizException(ErrorCode.USER_NOT_FOUND, "申请人不存在");
        }

        // 2. 校验申请凭据
        if (app.getUsername() == null || app.getUsername().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "申请缺少商家登录用户名");
        }
        if (app.getPassword() == null || app.getPassword().isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "申请缺少商家登录密码");
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
        if (app == null) throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, "申请不存在");
        return app;
    }

}
