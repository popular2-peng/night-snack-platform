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
import com.nsp.pojo.entity.Category;
import com.nsp.pojo.entity.Merchant;
import com.nsp.pojo.entity.MerchantCategory;
import com.nsp.pojo.entity.Order;
import com.nsp.pojo.entity.OrderItem;
import com.nsp.pojo.entity.OrderStatusLog;
import com.nsp.pojo.entity.PaymentRecord;
import com.nsp.pojo.entity.PlatformCategory;
import com.nsp.pojo.entity.Product;
import com.nsp.pojo.entity.ProductSpec;
import com.nsp.pojo.vo.AdminMerchantVO;
import com.nsp.pojo.vo.BatchResultVO;
import com.nsp.pojo.vo.MerchantDetailVO;
import com.nsp.pojo.vo.MerchantStatsVO;
import com.nsp.pojo.vo.PageVO;
import com.nsp.server.annotation.Loggable;
import com.nsp.server.mapper.CategoryMapper;
import com.nsp.server.mapper.MerchantCategoryMapper;
import com.nsp.server.mapper.MerchantMapper;
import com.nsp.server.mapper.OrderItemMapper;
import com.nsp.server.mapper.OrderMapper;
import com.nsp.server.mapper.OrderStatusLogMapper;
import com.nsp.server.mapper.PaymentRecordMapper;
import com.nsp.server.mapper.PlatformCategoryMapper;
import com.nsp.server.mapper.ProductMapper;
import com.nsp.server.mapper.ProductSpecMapper;
import com.nsp.server.cache.HomeMerchantListCache;
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
    private final HomeMerchantListCache homeMerchantListCache;
    private final PlatformCategoryMapper platformCategoryMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final CategoryMapper categoryMapper;
    private final MerchantCategoryMapper merchantCategoryMapper;

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
        homeMerchantListCache.evict();
        log.info("管理员封禁商家: merchantId={}", id);
    }

    @Override
    @Transactional
    @Loggable(action = "解封商家(简易)", targetType = "merchant")
    public void activateMerchant(Long id) {
        Merchant merchant = findById(id);
        merchant.setStatus("OPEN");
        merchantMapper.updateById(merchant);
        homeMerchantListCache.evict();
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
        homeMerchantListCache.evict();
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
        homeMerchantListCache.evict();
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
        homeMerchantListCache.evict();
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
        homeMerchantListCache.evict();
        log.info("商家解封: merchantId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional
    @Loggable(action = "删除商家", targetType = "merchant")
    public void softDelete(Long id, Long operatorId) {
        Merchant m = findById(id);
        if (!"SUSPENDED".equals(m.getStatus())) {
            throw new BizException(ErrorCode.MERCHANT_MUST_BANNED, MessageConst.MERCHANT_MUST_BANNED_MSG);
        }
        cascadeDeleteMerchant(id);
        homeMerchantListCache.evict();
        log.info("删除商家: merchantId={}, operatorId={}", id, operatorId);
    }

    @Override
    @Transactional
    public BatchResultVO batchDelete(List<Long> ids) {
        BatchResultVO result = new BatchResultVO();
        int notBanned = 0, missing = 0;
        for (Long id : ids) {
            Merchant m = merchantMapper.selectById(id);
            if (m == null) { missing++; continue; }
            if (!"SUSPENDED".equals(m.getStatus())) { notBanned++; continue; }
            cascadeDeleteMerchant(id);
            result.setSuccessCount(result.getSuccessCount() + 1);
            log.info("批量删除商家: merchantId={}", id);
        }
        result.setSkippedCount(notBanned + missing);
        if (notBanned > 0) result.addSkipped(notBanned + " 个商家未封禁不可删除");
        if (missing > 0) result.addSkipped(missing + " 个商家不存在");
        log.info("批量删除商家: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    /**
     * 级联物理删除商家及其关联数据（事务内）：
     * 订单 → 订单明细/状态日志/支付记录；商品 → 商品规格；商家内部分类；平台品类关联；商家本身
     * （入驻申请为独立业务数据，不随商家删除）
     */
    private void cascadeDeleteMerchant(Long merchantId) {
        // 1. 商家订单及其明细/日志/支付记录
        List<Order> orders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, merchantId));
        for (Order o : orders) {
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, o.getId()));
            orderStatusLogMapper.delete(new LambdaQueryWrapper<OrderStatusLog>()
                    .eq(OrderStatusLog::getOrderId, o.getId()));
            paymentRecordMapper.delete(new LambdaQueryWrapper<PaymentRecord>()
                    .eq(PaymentRecord::getOrderNo, o.getOrderNo()));
        }
        orderMapper.delete(new LambdaQueryWrapper<Order>().eq(Order::getMerchantId, merchantId));

        // 2. 商品及其规格
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));
        for (Product p : products) {
            productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>()
                    .eq(ProductSpec::getProductId, p.getId()));
        }
        productMapper.delete(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));

        // 3. 商家内部分类
        categoryMapper.delete(new LambdaQueryWrapper<Category>()
                .eq(Category::getMerchantId, merchantId));

        // 4. 平台品类关联
        merchantCategoryMapper.delete(new LambdaQueryWrapper<MerchantCategory>()
                .eq(MerchantCategory::getMerchantId, merchantId));

        // 5. 商家本身（物理删除）
        merchantMapper.deleteById(merchantId);
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
                .phone(m.getPhone())
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
                .updatedAt(m.getUpdatedAt())
                .build();
    }

}
