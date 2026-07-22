package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.AuditDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantApplication;
import com.nsp.pojo.entity.Order;
import com.nsp.pojo.entity.User;
import com.nsp.pojo.vo.*;
import com.nsp.server.mapper.*;
import com.nsp.server.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    // ──────────────────────────────────────
    // 首页统计
    // ──────────────────────────────────────

    @Override
    public AdminDashboardVO getDashboardStats() {
        Long totalUsers = userMapper.selectCount(null);
        Long totalMerchants = merchantMapper.selectCount(null);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayOrders = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().ge(Order::getCreatedAt, todayStart));

        List<Order> todayOrderList = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .ge(Order::getCreatedAt, todayStart)
                        .ne(Order::getStatus, com.nsp.common.enums.OrderStatus.CANCELLED));
        BigDecimal todayRevenue = todayOrderList.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AdminDashboardVO.builder()
                .totalUsers(totalUsers)
                .totalMerchants(totalMerchants)
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .build();
    }

    // ──────────────────────────────────────
    // 商家入驻审核
    // ──────────────────────────────────────

    @Override
    public PageVO<AdminApplicationVO> listApplications(int page, int size) {
        Page<MerchantApplication> result = merchantApplicationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<MerchantApplication>()
                        .eq(MerchantApplication::getStatus, "PENDING")
                        .orderByAsc(MerchantApplication::getCreatedAt));

        List<AdminApplicationVO> list = result.getRecords().stream().map(a -> {
            AdminApplicationVO vo = new AdminApplicationVO();
            BeanUtil.copyProperties(a, vo);
            User applicant = userMapper.selectById(a.getApplicantUserId());
            vo.setApplicantUsername(applicant != null ? applicant.getUsername() : "未知");
            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
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
            wrapper.like(Merchant::getName, keyword);
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
