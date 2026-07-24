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
import com.nsp.pojo.entity.*;
import com.nsp.pojo.vo.*;
import com.nsp.server.mapper.*;
import com.nsp.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理员业务实现
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final MerchantApplicationMapper merchantApplicationMapper;
    private final OrderMapper orderMapper;
    private final CategoryMapper categoryMapper;

    // ──────────────────────────────────────
    // 首页统计
    // ──────────────────────────────────────

    @Override
    public AdminDashboardVO getStats() {
        Long totalUsers = userMapper.selectCount(null);
        Long totalMerchants = merchantMapper.selectCount(null);
        Long totalOrders = orderMapper.selectCount(null);
        Long pendingApplications = merchantApplicationMapper.selectCount(
                new LambdaQueryWrapper<MerchantApplication>()
                        .eq(MerchantApplication::getStatus, "PENDING"));

        return AdminDashboardVO.builder()
                .totalUsers(totalUsers)
                .totalMerchants(totalMerchants)
                .totalOrders(totalOrders)
                .pendingApplications(pendingApplications)
                .build();
    }

    @Override
    public List<OrderStatusStatVO> getOrderStatusDistribution() {
        // 全表查 order 按 status 分组统计
        List<Order> all = orderMapper.selectList(null);

        // status → 中文名映射
        Map<String, String> labelMap = Map.of(
                "PENDING_PAY", "待支付",
                "PAID", "已支付",
                "ACCEPTED", "已接单",
                "COOKING", "制作中",
                "COMPLETED", "已完成",
                "CANCELLED", "已取消",
                "REFUNDING", "退款中",
                "REFUNDED", "已退款"
        );

        Map<String, Long> countMap = all.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getStatus().getCode(), Collectors.counting()));

        // 按固定顺序输出，保证前端图表一致
        List<String> orderedStatuses = List.of(
                "PENDING_PAY", "PAID", "ACCEPTED", "COOKING",
                "COMPLETED", "CANCELLED", "REFUNDING", "REFUNDED");

        List<OrderStatusStatVO> result = new ArrayList<>();
        for (String status : orderedStatuses) {
            Long count = countMap.getOrDefault(status, 0L);
            if (count > 0) {
                result.add(OrderStatusStatVO.builder()
                        .status(status)
                        .label(labelMap.getOrDefault(status, status))
                        .count(count)
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<AdminRecentOrderVO> getRecentOrders(int limit) {
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .orderByDesc(Order::getCreatedAt)
                        .last("LIMIT " + Math.min(limit, 50)));

        return orders.stream().map(o -> {
            String username = "";
            User user = userMapper.selectById(o.getUserId());
            if (user != null) username = user.getUsername();

            String merchantName = "";
            Merchant m = merchantMapper.selectById(o.getMerchantId());
            if (m != null) merchantName = m.getName();

            return AdminRecentOrderVO.builder()
                    .orderNo(o.getOrderNo())
                    .username(username)
                    .merchantName(merchantName)
                    .status(o.getStatus().getCode())
                    .totalAmount(o.getTotalAmount())
                    .createdAt(o.getCreatedAt())
                    .build();
        }).toList();
    }

    // ──────────────────────────────────────
    // 商家入驻审核
    // ──────────────────────────────────────

    @Override
    public PageVO<AdminApplicationVO> listApplications(int page, int size) {
        Page<MerchantApplication> result = merchantApplicationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<MerchantApplication>()
                        .orderByAsc(MerchantApplication::getCreatedAt));

        List<AdminApplicationVO> list = result.getRecords().stream().map(a -> {
            AdminApplicationVO vo = new AdminApplicationVO();
            BeanUtil.copyProperties(a, vo);

            User applicant = userMapper.selectById(a.getApplicantUserId());
            vo.setApplicantUsername(applicant != null ? applicant.getUsername() : "未知");
            vo.setPhone(applicant != null ? PhoneMaskUtil.mask(applicant.getPhone()) : "");

            // 品类名称
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
        MerchantApplication a = findApplication(id);
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
        MerchantApplication app = findApplication(id);

        app.setStatus("APPROVED");
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        if (dto.getReviewNote() != null) {
            app.setReviewNote(dto.getReviewNote());
        }
        merchantApplicationMapper.updateById(app);

        log.info("商家入驻申请通过(V2): appId={}, reviewerId={}, initialStatus={}, deliveryRadius={}",
                id, reviewerId, dto.getInitialStatus(), dto.getDeliveryRadius());
    }

    @Override
    @Transactional
    public void rejectApplicationV2(Long id, Long reviewerId, RejectApplicationDTO dto) {
        MerchantApplication app = findApplication(id);

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
        MerchantApplication app = findApplication(id);

        app.setStatus("APPROVED");
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        merchantApplicationMapper.updateById(app);

        log.info("商家入驻申请通过: appId={}, reviewerId={}", id, reviewerId);
    }

    @Override
    @Transactional
    public void rejectApplication(Long id, Long reviewerId, AuditDTO dto) {
        MerchantApplication app = findApplication(id);

        app.setStatus("REJECTED");
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());
        merchantApplicationMapper.updateById(app);

        log.info("商家入驻申请被拒绝: appId={}, reason={}", id, dto.getReason());
    }

    // ──────────────────────────────────────
    // 用户管理（管理顾客 user 表）
    // ──────────────────────────────────────

    @Override
    public PageVO<AdminUserVO> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                            .or().like(User::getPhone, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminUserVO> list = result.getRecords().stream().map(u -> {
            AdminUserVO vo = new AdminUserVO();
            BeanUtil.copyProperties(u, vo);
            vo.setRole("USER");
            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public void disableUser(Long id) {
        User user = findUser(id);
        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "用户已被封禁");
        }
        user.setStatus("DISABLED");
        userMapper.updateById(user);
        log.info("管理员封禁用户: userId={}", id);
    }

    @Override
    public void enableUser(Long id) {
        User user = findUser(id);
        if ("ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "用户未被封禁");
        }
        user.setStatus("ACTIVE");
        userMapper.updateById(user);
        log.info("管理员解封用户: userId={}", id);
    }

    // ──────────────────────────────────────
    // 商家管理
    // ──────────────────────────────────────

    @Override
    public PageVO<AdminMerchantVO> listMerchants(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Merchant::getName, keyword)
                            .or().like(Merchant::getUsername, keyword)
                            .or().like(Merchant::getPhone, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Merchant::getStatus, status.toUpperCase());
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);

        Page<Merchant> result = merchantMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminMerchantVO> list = result.getRecords().stream().map(m -> {
            AdminMerchantVO vo = new AdminMerchantVO();
            BeanUtil.copyProperties(m, vo);
            vo.setOwnerName(m.getUsername());
            vo.setPhone(PhoneMaskUtil.mask(m.getPhone()));

            if (m.getCategoryId() != null) {
                Category c = categoryMapper.selectById(m.getCategoryId());
                vo.setCategoryName(c != null ? c.getName() : "");
            }

            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public void suspendMerchant(Long id) {
        Merchant merchant = findMerchant(id);
        merchant.setStatus("SUSPENDED");
        merchantMapper.updateById(merchant);
        log.info("管理员封禁商家: merchantId={}", id);
    }

    @Override
    public void activateMerchant(Long id) {
        Merchant merchant = findMerchant(id);
        merchant.setStatus("OPEN");
        merchantMapper.updateById(merchant);
        log.info("管理员解封商家: merchantId={}", id);
    }

    // ──────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────

    private MerchantApplication findApplication(Long id) {
        MerchantApplication app = merchantApplicationMapper.selectById(id);
        if (app == null) throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, "申请不存在");
        return app;
    }

    private User findUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        return user;
    }

    private Merchant findMerchant(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, "商家不存在");
        return m;
    }

}
