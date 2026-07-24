package com.nsp.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.exception.BizException;
import com.nsp.common.util.IdCardMaskUtil;
import com.nsp.common.util.PhoneMaskUtil;
import com.nsp.pojo.dto.BanMerchantDTO;
import com.nsp.pojo.entity.Category;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.vo.MerchantDetailVO;
import com.nsp.pojo.vo.MerchantStatsVO;
import com.nsp.server.annotation.Loggable;
import com.nsp.server.mapper.CategoryMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.service.MerchantAdminService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 商家管理业务实现
 */
@Service
@RequiredArgsConstructor
public class MerchantAdminServiceImpl implements MerchantAdminService {

    private static final Logger log = LoggerFactory.getLogger(MerchantAdminServiceImpl.class);

    private final MerchantMapper merchantMapper;
    private final CategoryMapper categoryMapper;

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
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "商家已处于打烊状态");
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
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "商家已在营业中");
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
            throw new BizException(ErrorCode.MERCHANT_ALREADY_BANNED, "商家已被封禁，无需重复操作");
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
            throw new BizException(ErrorCode.MERCHANT_NOT_BANNED, "商家未被封禁，无法解封");
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
        if (m == null) throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, "商家不存在");
        return m;
    }

    private MerchantDetailVO buildDetailVO(Merchant m) {
        String categoryName = "";
        if (m.getCategoryId() != null) {
            Category c = categoryMapper.selectById(m.getCategoryId());
            if (c != null) categoryName = c.getName();
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
                .todayOrders(m.getTodayOrders())
                .todayRevenue(m.getTodayRevenue())
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
