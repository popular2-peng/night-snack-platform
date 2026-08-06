package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.exception.BizException;
import com.nsp.common.util.IdCardMaskUtil;
import com.nsp.common.util.PhoneMaskUtil;
import com.nsp.pojo.dto.BanMerchantDTO;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.PlatformCategory;
import com.nsp.pojo.vo.AdminMerchantVO;
import com.nsp.pojo.vo.MerchantDetailVO;
import com.nsp.pojo.vo.MerchantStatsVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.Loggable;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.PlatformCategoryMapper;
import com.nsp.server.mapper.OrderMapper;
import com.nsp.server.service.MerchantAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商家管理业务实现
 */
@Service
@RequiredArgsConstructor
public class MerchantAdminServiceImpl implements MerchantAdminService {

    private static final Logger log = LoggerFactory.getLogger(MerchantAdminServiceImpl.class);

    private final MerchantMapper merchantMapper;
    private final PlatformCategoryMapper platformCategoryMapper;
    private final OrderMapper orderMapper;

    // ── 商家列表 ──

    @Override
    public PageVO<AdminMerchantVO> listMerchants(int page, int size, String keyword, String status,
                                                Long categoryId, String dateStart, String dateEnd) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Merchant::getName, keyword)
                            .or().like(Merchant::getUsername, keyword)
                            .or().like(Merchant::getPhone, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(Merchant::getStatus, status.toUpperCase());
        }
        // 品类筛选
        if (categoryId != null) {
            wrapper.eq(Merchant::getCategoryId, categoryId);
        }
        // 入驻时间范围
        if (dateStart != null && !dateStart.isBlank()) {
            wrapper.ge(Merchant::getCreatedAt, LocalDateTime.parse(dateStart + "T00:00:00"));
        }
        if (dateEnd != null && !dateEnd.isBlank()) {
            wrapper.le(Merchant::getCreatedAt, LocalDateTime.parse(dateEnd + "T23:59:59"));
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);

        Page<Merchant> result = merchantMapper.selectPage(new Page<>(page, size), wrapper);
        List<AdminMerchantVO> list = result.getRecords().stream().map(m -> {
            AdminMerchantVO vo = new AdminMerchantVO();
            BeanUtil.copyProperties(m, vo);
            vo.setOwnerName(m.getUsername());
            vo.setPhone(PhoneMaskUtil.mask(m.getPhone()));

            if (m.getCategoryId() != null) {
                PlatformCategory c = platformCategoryMapper.selectById(m.getCategoryId());
                vo.setCategoryName(c != null ? c.getName() : "");
            }

            return vo;
        }).toList();

        // 从 order 表实时计算各商家今日订单数/营收
        if (!list.isEmpty()) {
            List<Long> merchantIds = list.stream().map(AdminMerchantVO::getId).toList();
            List<Map<String, Object>> statsList = orderMapper.selectTodayStatsBatch(merchantIds);
            Map<Long, Map<String, Object>> statsMap = statsList.stream()
                    .collect(Collectors.toMap(
                            m -> ((Number) m.get("merchant_id")).longValue(),
                            m -> m
                    ));
            list.forEach(vo -> {
                Map<String, Object> stats = statsMap.get(vo.getId());
                if (stats != null) {
                    vo.setTodayOrders(((Number) stats.get("order_count")).intValue());
                    vo.setTodayRevenue(new java.math.BigDecimal(stats.get("revenue").toString()));
                } else {
                    vo.setTodayOrders(0);
                    vo.setTodayRevenue(java.math.BigDecimal.ZERO);
                }
            });
        }

        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    @Transactional
    @Loggable(action = "封禁商家(简易)", targetType = "merchant")
    public void suspendMerchant(Long id) {
        Merchant merchant = findById(id);
        merchant.setStatus("SUSPENDED");
        merchantMapper.updateById(merchant);
        log.info("管理员封禁商家: merchantId={}", id);
    }

    @Override
    @Transactional
    @Loggable(action = "解封商家(简易)", targetType = "merchant")
    public void activateMerchant(Long id) {
        Merchant merchant = findById(id);
        merchant.setStatus("OPEN");
        merchantMapper.updateById(merchant);
        log.info("管理员解封商家: merchantId={}", id);
    }

    // ── 商家统计 ──

    @Override
    public MerchantStatsVO getStats() {
        List<Merchant> all = merchantMapper.selectList(null);
        long total = all.size();
        long openCount = all.stream().filter(m -> "OPEN".equals(m.getStatus())).count();
        long pendingCount = all.stream().filter(m -> "PENDING".equals(m.getStatus())).count();
        long suspendedCount = all.stream().filter(m -> "SUSPENDED".equals(m.getStatus())).count();
        long closedCount = all.stream().filter(m -> "CLOSED".equals(m.getStatus())).count();

        return MerchantStatsVO.builder()
                .total(total)
                .openCount(openCount)
                .pendingCount(pendingCount)
                .suspendedCount(suspendedCount)
                .closedCount(closedCount)
                .build();
    }

    @Override
    public MerchantDetailVO getDetail(Long id) {
        Merchant m = findById(id);
        return buildDetailVO(m);
    }

    @Override
    @Transactional
    @Loggable(action = "暂停营业", targetType = "merchant")
    public void pause(Long id, Long operatorId) {
        Merchant m = findById(id);
        if ("CLOSED".equals(m.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, MessageConst.MERCHANT_CLOSED_STATUS_MSG);
        }
        m.setStatus("CLOSED");
        m.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(m);
        log.info("商家暂停营业: merchantId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional
    @Loggable(action = "恢复营业", targetType = "merchant")
    public void resume(Long id, Long operatorId) {
        Merchant m = findById(id);
        if ("OPEN".equals(m.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, MessageConst.MERCHANT_OPEN_STATUS_MSG);
        }
        m.setStatus("OPEN");
        m.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(m);
        log.info("商家恢复营业: merchantId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional
    @Loggable(action = "封禁商家", targetType = "merchant")
    public void ban(Long id, Long operatorId, BanMerchantDTO dto) {
        Merchant m = findById(id);
        if ("SUSPENDED".equals(m.getStatus())) {
            throw new BizException(ErrorCode.MERCHANT_ALREADY_BANNED, MessageConst.MERCHANT_ALREADY_BANNED_MSG);
        }
        m.setStatus("SUSPENDED");
        m.setBannedReason(dto.getReason());
        m.setBannedAt(LocalDateTime.now());
        m.setBannedBy(operatorId);
        m.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(m);
        log.info("商家被封禁: merchantId={}, reason={}, operatorId={}", id, dto.getReason(), operatorId);
    }

    @Override
    @Transactional
    @Loggable(action = "解封商家", targetType = "merchant")
    public void unban(Long id, Long operatorId) {
        Merchant m = findById(id);
        if (!"SUSPENDED".equals(m.getStatus())) {
            throw new BizException(ErrorCode.MERCHANT_NOT_BANNED, MessageConst.MERCHANT_NOT_BANNED_MSG);
        }
        m.setStatus("OPEN");
        m.setBannedReason(null);
        m.setBannedAt(null);
        m.setBannedBy(null);
        m.setUpdatedAt(LocalDateTime.now());
        merchantMapper.updateById(m);
        log.info("商家解封: merchantId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional
    @Loggable(action = "删除商家", targetType = "merchant")
    public void softDelete(Long id, Long operatorId) {
        Merchant m = findById(id);
        merchantMapper.deleteById(m.getId());
        log.info("商家软删: merchantId={}, operatorId={}", id, operatorId);
    }

    @Override
    public List<Map<String, Object>> getOperationLogs(Long id) {
        // 暂无独立操作日志表，后续建表后补全
        // 当前返回商家的关键时间节点
        Merchant m = findById(id);
        List<Map<String, Object>> logs = new ArrayList<>();

        Map<String, Object> created = new LinkedHashMap<>();
        created.put("time", m.getCreatedAt());
        created.put("action", "入驻");
        created.put("detail", "商家入驻平台");
        logs.add(created);

        if (m.getBannedAt() != null) {
            Map<String, Object> banned = new LinkedHashMap<>();
            banned.put("time", m.getBannedAt());
            banned.put("action", "封禁");
            banned.put("detail", m.getBannedReason());
            logs.add(banned);
        }

        if (m.getLastLoginAt() != null) {
            Map<String, Object> login = new LinkedHashMap<>();
            login.put("time", m.getLastLoginAt());
            login.put("action", "商家登录");
            login.put("detail", "商家最后登录");
            logs.add(login);
        }

        logs.sort((a, b) -> {
            Object ta = a.get("time");
            Object tb = b.get("time");
            if (ta == null || tb == null) return 0;
            return ((Comparable) tb).compareTo(ta);
        });
        return logs;
    }

    // ---- 内部方法 ----

    private Merchant findById(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null) throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, MessageConst.MERCHANT_NOT_FOUND_MSG);
        return m;
    }

    private MerchantDetailVO buildDetailVO(Merchant m) {
        String categoryName = "";
        if (m.getCategoryId() != null) {
            PlatformCategory c = platformCategoryMapper.selectById(m.getCategoryId());
            if (c != null) categoryName = c.getName();
        }

        // 从 order 表实时计算今日订单数/营收
        Map<String, Object> todayStats = orderMapper.selectTodayStats(m.getId());
        int todayOrders = 0;
        java.math.BigDecimal todayRevenue = java.math.BigDecimal.ZERO;
        if (todayStats != null && !todayStats.isEmpty()) {
            Object orderCount = todayStats.get("order_count");
            Object revenue = todayStats.get("revenue");
            if (orderCount != null) todayOrders = ((Number) orderCount).intValue();
            if (revenue != null) todayRevenue = new java.math.BigDecimal(revenue.toString());
        }

        return MerchantDetailVO.builder()
                .id(m.getId())
                .username(m.getUsername())
                .name(m.getName())
                .phone(PhoneMaskUtil.mask(m.getPhone()))
                .address(m.getAddress())
                .lat(m.getLat())
                .lng(m.getLng())
                .logoUrl(m.getLogoUrl())
                .categoryName(categoryName)
                .description(m.getDescription())
                .idCardNo(IdCardMaskUtil.mask(m.getIdCardNo()))
                .avgRating(m.getAvgRating())
                .totalOrders(m.getTotalOrders())
                .todayOrders(todayOrders)
                .todayRevenue(todayRevenue)
                .status(m.getStatus())
                .bannedReason(m.getBannedReason())
                .bannedAt(m.getBannedAt())
                .notice(m.getNotice())
                .businessHoursStart(m.getBusinessHoursStart())
                .businessHoursEnd(m.getBusinessHoursEnd())
                .deliveryRadius(m.getDeliveryRadius())
                .lastLoginAt(m.getLastLoginAt())
                .createdAt(m.getCreatedAt())
                .build();
    }

}
