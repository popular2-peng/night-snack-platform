package com.nsp.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.enums.OrderStatus;
import com.nsp.common.enums.PayChannel;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.OrderCreateDTO;
import com.nsp.pojo.entity.*;
import com.nsp.pojo.vo.*;
import com.nsp.server.mapper.*;
import com.nsp.server.service.CartService;
import com.nsp.server.service.MapService;
import com.nsp.server.service.OrderService;
import com.nsp.server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单业务实现 — 状态机 + Lua 库存预扣 + 地址快照
 *
 * <p>⚡ 库存扣减必须使用 Redis Lua 脚本保证原子性，禁止 Java 先读后写
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final ProductMapper productMapper;
    private final ProductSpecMapper productSpecMapper;
    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final StringRedisTemplate redisTemplate;
    private final MapService mapService;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    /** 订单号日期前缀（yyyyMMdd，Redis 自增序号键按天分桶） */
    private static final DateTimeFormatter ORDER_NO_DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 降级路径用的完整时间戳格式 */
    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 自增序号键 TTL（天） */
    private static final long ORDER_NO_KEY_TTL_DAYS = 2;
    /** 下单幂等键 TTL（分钟）：覆盖正常下单耗时，超时后允许重建 */
    private static final long IDEMPOTENCY_KEY_TTL_MINUTES = 10;
    /** 幂等键占位值：首个请求处理中 */
    private static final String IDEMPOTENCY_PENDING = "PENDING";

    // ──────────────────────────────────────
    // 下单
    // ──────────────────────────────────────

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
        // 0. 幂等拦截（必须在库存扣减之前，否则并发重复请求会二次扣减库存）：
        //    同一 clientToken 首次请求占位 PENDING，成功后改写为订单号（重复请求直接返回该订单）；
        //    占位期间（首个请求仍在处理）的重复请求抛 4006「订单创建中」。
        String idempotencyKey = "order:client-token:" + userId + ":" + dto.getClientToken();
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, IDEMPOTENCY_PENDING, Duration.ofMinutes(IDEMPOTENCY_KEY_TTL_MINUTES));
        if (!Boolean.TRUE.equals(first)) {
            String existing = redisTemplate.opsForValue().get(idempotencyKey);
            if (existing != null && !IDEMPOTENCY_PENDING.equals(existing)) {
                // 幂等命中：返回已创建的订单（同一 clientToken 的重复请求得到同一订单）
                Order existOrder = orderMapper.selectOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, existing));
                if (existOrder != null) {
                    log.info("下单幂等命中（返回已创建订单）: userId={}, clientToken={}, orderNo={}",
                            userId, dto.getClientToken(), existing);
                    return toVO(existOrder);
                }
                // 键存在但订单已被删除 → 释放键，按新订单处理
                redisTemplate.delete(idempotencyKey);
            } else {
                throw new BizException(ErrorCode.ORDER_CREATING, MessageConst.ORDER_CREATING_MSG);
            }
        }

        try {
        // 1. 校验商家
        Merchant merchant = merchantMapper.selectById(dto.getMerchantId());
        if (merchant == null) {
            throw new BizException(ErrorCode.MERCHANT_NOT_FOUND, MessageConst.MERCHANT_NOT_FOUND_MSG);
        }
        if (!"OPEN".equals(merchant.getStatus())) {
            throw new BizException(ErrorCode.MERCHANT_CLOSED, MessageConst.MERCHANT_CLOSED_MSG);
        }
        validateBusinessHours(merchant);

        // 2. 校验配送范围（以地址坐标为准）
        UserAddress address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ADDRESS_NOT_FOUND, MessageConst.ORDER_ADDRESS_NOT_FOUND_MSG);
        }
        long distance = mapService.calculateDistance(
                merchant.getLat(), merchant.getLng(),
                address.getLat(), address.getLng());
        if (distance > merchant.getDeliveryRadius()) {
            throw new BizException(ErrorCode.OUT_OF_DELIVERY_RANGE,
                    String.format(MessageConst.ORDER_OUT_OF_DELIVERY_RANGE_MSG, distance, merchant.getDeliveryRadius()));
        }

        // 3. 校验订单明细，所有商品必须属于同一商家
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<ProductSpec> specs = new ArrayList<>();
        List<Product> products = new ArrayList<>();
        Map<Long, Integer> specQtyMap = new LinkedHashMap<>();

        for (OrderCreateDTO.OrderItemDTO item : dto.getItems()) {
            ProductSpec spec = productSpecMapper.selectById(item.getSpecId());
            if (spec == null) {
                throw new BizException(ErrorCode.SPEC_NOT_FOUND, String.format(MessageConst.SPEC_NOT_FOUND_WITH_ID_MSG, item.getSpecId()));
            }
            Product product = productMapper.selectById(spec.getProductId());
            if (product == null || !product.getMerchantId().equals(dto.getMerchantId())) {
                throw new BizException(ErrorCode.PRODUCT_NOT_FOUND,
                        String.format(MessageConst.PRODUCT_NOT_BELONG_MERCHANT_MSG, item.getSpecId()));
            }
            if (!"ON".equals(product.getStatus())) {
                throw new BizException(ErrorCode.PRODUCT_OFF_SHELF, String.format(MessageConst.PRODUCT_OFF_SHELF_MSG, product.getName()));
            }

            specs.add(spec);
            products.add(product);
            specQtyMap.put(item.getSpecId(), item.getQuantity());

            BigDecimal unitPrice = product.getPrice().add(spec.getPriceDelta());
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // 4. Lua 原子扣减库存
        for (OrderCreateDTO.OrderItemDTO item : dto.getItems()) {
            deductStock(item.getSpecId(), item.getQuantity());
        }

        // 5. 生成订单号
        String orderNo = generateOrderNo();

        // 6. 地址快照
        String addressSnapshot = toJson(address);

        // 7. 写入数据库
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchant.getId());
        order.setStatus(OrderStatus.PENDING_PAY);
        order.setTotalAmount(totalAmount);
        order.setAddressSnapshot(addressSnapshot);
        order.setPayChannel(PayChannel.fromCode(dto.getPayChannel()));
        orderMapper.insert(order);

        // created_at 由数据库默认值（CURRENT_TIMESTAMP）填充，insert 后重新加载以回填
        order = orderMapper.selectById(order.getId());

        for (int i = 0; i < specs.size(); i++) {
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(products.get(i).getId());
            oi.setSpecId(specs.get(i).getId());
            oi.setNameSnapshot(products.get(i).getName() + " / " + specs.get(i).getName());
            oi.setPriceSnapshot(products.get(i).getPrice().add(specs.get(i).getPriceDelta()));
            oi.setQuantity(dto.getItems().get(i).getQuantity());
            orderItemMapper.insert(oi);
        }

        // 记录状态日志
        saveStatusLog(order.getId(), null, OrderStatus.PENDING_PAY.getCode(), "USER", "用户下单");

        // 8. 清空购物车
        cartService.clearCart(userId);

        log.info("下单成功: orderNo={}, userId={}, merchantId={}, amount={}",
                orderNo, userId, merchant.getId(), totalAmount);

        // 幂等键改写为订单号：此后同一 clientToken 的重复请求直接返回该订单
        redisTemplate.opsForValue().set(idempotencyKey, orderNo, Duration.ofMinutes(IDEMPOTENCY_KEY_TTL_MINUTES));
        return toVO(order);
        } catch (Exception e) {
            // Redis 操作不参与事务回滚，异常路径必须手动释放幂等键，否则用户 10 分钟内无法重试
            try {
                redisTemplate.delete(idempotencyKey);
            } catch (Exception redisEx) {
                log.warn("幂等键释放失败（依赖 TTL 兜底）: key={}", idempotencyKey);
            }
            throw e;
        }
    }

    // ──────────────────────────────────────
    // 订单查询
    // ──────────────────────────────────────

    @Override
    public PageVO<OrderListVO> getUserOrders(Long userId, int page, int size) {
        Page<Order> result = orderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getId));

        return toPageVO(result);
    }

    @Override
    public PageVO<OrderListVO> getMerchantOrders(Long merchantId, Long userId, int page, int size, String status) {
        // 校验 userId 是该商家店主
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || !merchant.getId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, MessageConst.NOT_MERCHANT_ORDER_VIEW_MSG);
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getMerchantId, merchantId)
                .orderByDesc(Order::getId);

        // 状态筛选（非法状态值忽略，不拼条件）
        if (status != null && !status.isBlank()) {
            OrderStatus statusEnum = OrderStatus.fromCode(status);
            if (statusEnum != null) {
                wrapper.eq(Order::getStatus, statusEnum);
            }
        }

        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);

        return toPageVO(result);
    }

    @Override
    public Map<String, Long> getMerchantOrderStats(Long merchantId, Long userId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || !merchant.getId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, MessageConst.NOT_MERCHANT_ORDER_VIEW_MSG);
        }
        Map<String, Long> stats = new HashMap<>();
        for (Map<String, Object> row : orderMapper.selectStatusStats(merchantId)) {
            Object statusObj = row.get("status");
            Object cntObj = row.get("cnt");
            if (statusObj != null && cntObj != null) {
                stats.put(statusObj.toString(), Long.parseLong(cntObj.toString()));
            }
        }
        return stats;
    }

    @Override
    public PageVO<AdminOrderVO> getAdminOrders(int page, int size, String keyword,
                                               String status, String payChannel,
                                               String dateStart, String dateEnd) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Order::getId);

        // 关键词：订单号模糊 / 下单用户名模糊 / 商家名模糊（后两者用子查询关联 user、merchant 表）
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Order::getOrderNo, keyword)
                    .or().inSql(Order::getUserId,
                            "SELECT id FROM user WHERE username LIKE '%" + keyword + "%'")
                    .or().inSql(Order::getMerchantId,
                            "SELECT id FROM merchant WHERE name LIKE '%" + keyword + "%'"));
        }

        // 状态筛选（非法状态值忽略，不拼条件）
        if (status != null && !status.isBlank()) {
            OrderStatus statusEnum = OrderStatus.fromCode(status);
            if (statusEnum != null) {
                wrapper.eq(Order::getStatus, statusEnum);
            }
        }

        // 支付渠道筛选（ALIPAY / WECHAT_MOCK，非法值忽略）
        if (payChannel != null && !payChannel.isBlank()) {
            PayChannel channelEnum = PayChannel.fromCode(payChannel);
            if (channelEnum != null) {
                wrapper.eq(Order::getPayChannel, channelEnum);
            }
        }

        // 下单时间范围（含边界当天）
        if (dateStart != null && !dateStart.isBlank()) {
            wrapper.ge(Order::getCreatedAt, LocalDate.parse(dateStart).atStartOfDay());
        }
        if (dateEnd != null && !dateEnd.isBlank()) {
            wrapper.le(Order::getCreatedAt, LocalDate.parse(dateEnd).atTime(LocalTime.MAX));
        }

        Page<Order> result = orderMapper.selectPage(new Page<>(page, size), wrapper);

        List<AdminOrderVO> list = result.getRecords().stream().map(o -> {
            AdminOrderVO vo = new AdminOrderVO();
            BeanUtil.copyProperties(o, vo);
            vo.setStatus(o.getStatus().getCode());
            Merchant m = merchantMapper.selectById(o.getMerchantId());
            vo.setMerchantName(m != null ? m.getName() : "");
            User u = userMapper.selectById(o.getUserId());
            vo.setUsername(u != null ? u.getUsername() : "");
            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    @Override
    public OrderVO getOrderDetail(Long orderId, Long userId, String role) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, MessageConst.ORDER_NOT_FOUND_MSG);
        }

        // 越权检查
        if ("USER".equals(role) && !order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, MessageConst.ORDER_VIEW_FORBIDDEN_MSG);
        }
        if ("MERCHANT".equals(role)) {
            Merchant merchant = merchantMapper.selectById(order.getMerchantId());
            if (merchant == null || !merchant.getId().equals(userId)) {
                throw new BizException(ErrorCode.FORBIDDEN, MessageConst.ORDER_VIEW_FORBIDDEN_MSG);
            }
        }

        return toVO(order);
    }

    // ──────────────────────────────────────
    // 状态流转
    // ──────────────────────────────────────

    @Override
    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Order order = findOwnOrder(orderId, userId);
        assertStatus(order, OrderStatus.PENDING_PAY, MessageConst.ORDER_STATUS_CANCEL_MSG);

        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);

        // 释放库存
        restoreStock(orderId);

        saveStatusLog(orderId, OrderStatus.PENDING_PAY.getCode(),
                OrderStatus.CANCELLED.getCode(), "USER", "用户取消订单");
        log.info("订单已取消: orderId={}, orderNo={}", orderId, order.getOrderNo());
    }

    @Override
    @Transactional
    public void acceptOrder(Long orderId, Long merchantId, Long userId) {
        Order order = findMerchantOrder(orderId, merchantId, userId);
        assertStatus(order, OrderStatus.PAID, MessageConst.ORDER_STATUS_ACCEPT_MSG);

        order.setStatus(OrderStatus.ACCEPTED);
        orderMapper.updateById(order);

        saveStatusLog(orderId, OrderStatus.PAID.getCode(),
                OrderStatus.ACCEPTED.getCode(), "MERCHANT", "商家接单");
        log.info("商家接单: orderId={}, orderNo={}", orderId, order.getOrderNo());
    }

    @Override
    @Transactional
    public void startCooking(Long orderId, Long merchantId, Long userId) {
        Order order = findMerchantOrder(orderId, merchantId, userId);
        assertStatus(order, OrderStatus.ACCEPTED, MessageConst.ORDER_STATUS_COOKING_MSG);

        order.setStatus(OrderStatus.COOKING);
        orderMapper.updateById(order);

        saveStatusLog(orderId, OrderStatus.ACCEPTED.getCode(),
                OrderStatus.COOKING.getCode(), "MERCHANT", "开始制作");
        log.info("开始制作: orderId={}, orderNo={}", orderId, order.getOrderNo());
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId, Long merchantId, Long userId) {
        Order order = findMerchantOrder(orderId, merchantId, userId);
        assertStatus(order, OrderStatus.COOKING, MessageConst.ORDER_STATUS_COMPLETE_MSG);

        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);

        saveStatusLog(orderId, OrderStatus.COOKING.getCode(),
                OrderStatus.COMPLETED.getCode(), "MERCHANT", "制作完成");
        log.info("订单完成: orderId={}, orderNo={}", orderId, order.getOrderNo());
    }

    @Override
    @Transactional
    public void rejectOrder(Long orderId, Long merchantId, Long userId, String reason) {
        Order order = findMerchantOrder(orderId, merchantId, userId);
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, MessageConst.ORDER_STATUS_REJECT_MSG);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);

        // 释放库存
        restoreStock(orderId);

        // 自动退款（支付宝沙箱真实退款 / 微信 mock 退款）；退款失败不阻断拒单主流程，可由管理员批量退款重试
        try {
            paymentService.refund(order.getOrderNo());
        } catch (Exception e) {
            log.error("商家拒单后自动退款失败（可重试）: orderId={}, orderNo={}", orderId, order.getOrderNo(), e);
        }

        saveStatusLog(orderId, OrderStatus.PAID.getCode(),
                OrderStatus.CANCELLED.getCode(), "MERCHANT", "商家拒单: " + reason);
        log.info("商家拒单: orderId={}, orderNo={}, reason={}", orderId, order.getOrderNo(), reason);
    }

    // ──────────────────────────────────────
    // 超时自动任务（定时扫描调用，见 OrderTimeoutScheduler）
    // ──────────────────────────────────────

    @Override
    @Transactional
    public void timeoutCancel(Long orderId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, MessageConst.ORDER_NOT_FOUND_MSG);
        }
        // 仅待支付 / 已支付（未接单）可被超时取消
        if (order.getStatus() != OrderStatus.PENDING_PAY && order.getStatus() != OrderStatus.PAID) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, MessageConst.ORDER_STATUS_CANCEL_MSG);
        }
        OrderStatus fromStatus = order.getStatus();
        boolean paid = fromStatus == OrderStatus.PAID;

        order.setStatus(OrderStatus.CANCELLED);
        orderMapper.updateById(order);

        // 释放预扣库存
        restoreStock(orderId);

        // 已支付订单超时取消 → 自动退款（支付宝沙箱真实退款 / 微信 mock 退款）；
        // 退款失败仅记录日志不抛异常，订单保持 CANCELLED，可由管理员批量退款重试
        if (paid) {
            try {
                paymentService.refund(order.getOrderNo());
                log.info("超时未接单自动退款: orderNo={}", order.getOrderNo());
            } catch (Exception e) {
                log.error("超时取消自动退款失败（可重试）: orderId={}, orderNo={}", orderId, order.getOrderNo(), e);
            }
        }

        saveStatusLog(orderId, fromStatus.getCode(),
                OrderStatus.CANCELLED.getCode(), "system", reason);
        log.info("订单超时自动取消: orderId={}, orderNo={}, reason={}", orderId, order.getOrderNo(), reason);
    }

    @Override
    @Transactional
    public void timeoutComplete(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, MessageConst.ORDER_NOT_FOUND_MSG);
        }
        if (order.getStatus() != OrderStatus.COOKING) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, MessageConst.ORDER_STATUS_COMPLETE_MSG);
        }

        order.setStatus(OrderStatus.COMPLETED);
        orderMapper.updateById(order);

        saveStatusLog(orderId, OrderStatus.COOKING.getCode(),
                OrderStatus.COMPLETED.getCode(), "system", "配送超时自动确认送达");
        log.info("订单超时自动完成: orderId={}, orderNo={}", orderId, order.getOrderNo());
    }

    // ──────────────────────────────────────
    // 批量删除（管理端）
    // ──────────────────────────────────────

    @Override
    @Transactional
    public BatchResultVO batchDelete(List<Long> ids) {
        BatchResultVO result = new BatchResultVO();
        int missing = 0;
        for (Long id : ids) {
            Order order = orderMapper.selectById(id);
            if (order == null) { missing++; continue; }

            // 级联删除：订单明细 / 状态流转日志 / 支付记录（payment_record 无外键，按 order_no 关联）
            orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
            orderStatusLogMapper.delete(new LambdaQueryWrapper<OrderStatusLog>().eq(OrderStatusLog::getOrderId, id));
            paymentRecordMapper.delete(new LambdaQueryWrapper<PaymentRecord>().eq(PaymentRecord::getOrderNo, order.getOrderNo()));
            orderMapper.deleteById(id);

            result.setSuccessCount(result.getSuccessCount() + 1);
            log.info("批量删除订单: id={}, orderNo={}", id, order.getOrderNo());
        }
        result.setSkippedCount(missing);
        if (missing > 0) result.addSkipped(missing + " 条订单不存在");
        log.info("批量删除订单: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    // ──────────────────────────────────────
    // 管理员退款（主动退款 / 批量退款）
    // ──────────────────────────────────────

    /**
     * 管理员退款 — 状态流转：PAID/ACCEPTED/COOKING/REFUNDING → REFUNDING →（退款成功）REFUNDED
     *
     * <p>不加 @Transactional：渠道退款是外部 IO 调用，不宜包在长事务里；
     * REFUNDING 本身是合法的中间态，失败后可重试（复用同一退款请求号，渠道侧幂等）。
     */
    @Override
    public OrderVO refundOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, MessageConst.ORDER_NOT_FOUND_MSG);
        }

        OrderStatus status = order.getStatus();
        if (status == OrderStatus.PENDING_PAY) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, MessageConst.ORDER_NOT_PAID_REFUND_MSG);
        }
        boolean retry = status == OrderStatus.REFUNDING;
        if (!retry && status != OrderStatus.PAID && status != OrderStatus.ACCEPTED && status != OrderStatus.COOKING) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR,
                    String.format(MessageConst.ORDER_STATUS_REFUND_MSG, status.getDesc()));
        }

        // 首次退款：置 REFUNDING + 记录流转日志（重试时不再重复记录）
        if (!retry) {
            order.setStatus(OrderStatus.REFUNDING);
            orderMapper.updateById(order);
            saveStatusLog(orderId, status.getCode(), OrderStatus.REFUNDING.getCode(), "ADMIN", "管理员发起退款");
            log.info("管理员发起退款: orderId={}, orderNo={}", orderId, order.getOrderNo());
        } else {
            log.info("管理员重试退款: orderId={}, orderNo={}", orderId, order.getOrderNo());
        }

        // 渠道退款（失败抛 5007，订单保持 REFUNDING 可重试）
        paymentService.refund(order.getOrderNo());

        // 退款成功：置 REFUNDED + 记录流转日志
        order = orderMapper.selectById(orderId);
        order.setStatus(OrderStatus.REFUNDED);
        orderMapper.updateById(order);
        saveStatusLog(orderId, OrderStatus.REFUNDING.getCode(), OrderStatus.REFUNDED.getCode(), "ADMIN", "退款成功");
        log.info("管理员退款完成: orderId={}, orderNo={}", orderId, order.getOrderNo());

        return toVO(order);
    }

    @Override
    public BatchResultVO batchRefund(List<Long> ids) {
        BatchResultVO result = new BatchResultVO();
        Map<String, Integer> skipReasonCounts = new LinkedHashMap<>();
        for (Long id : ids) {
            try {
                refundOrder(id);
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (BizException e) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                skipReasonCounts.merge(e.getMessage(), 1, Integer::sum);
            }
        }
        skipReasonCounts.forEach((reason, count) ->
                result.addSkipped(count + " 条" + reason.replace("订单", "")));
        log.info("批量退款: 成功={}, 跳过={}", result.getSuccessCount(), result.getSkippedCount());
        return result;
    }

    // ──────────────────────────────────────
    // 内部方法 — 库存
    // ──────────────────────────────────────

    /** Lua 原子扣减库存 */
    private void deductStock(Long specId, int qty) {
        String key = "product:stock:" + specId;

        // 如果 Redis 中不存在，从 MySQL 初始化
        String existingStock = redisTemplate.opsForValue().get(key);
        if (existingStock == null) {
            ProductSpec spec = productSpecMapper.selectById(specId);
            if (spec == null) throw new BizException(ErrorCode.SPEC_NOT_FOUND, MessageConst.SPEC_NOT_FOUND_MSG);
            redisTemplate.opsForValue().set(key, String.valueOf(spec.getStock()));
        }

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/stock_deduct.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, List.of(key), String.valueOf(qty));
        if (result == null) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, MessageConst.SYSTEM_STOCK_SCRIPT_ERROR_MSG);
        }
        if (result == -1L) {
            throw new BizException(ErrorCode.STOCK_INSUFFICIENT, String.format(MessageConst.STOCK_INSUFFICIENT_MSG, specId));
        }
        if (result == -2L) {
            throw new BizException(ErrorCode.SPEC_NOT_FOUND, String.format(MessageConst.STOCK_NOT_INITIALIZED_MSG, specId));
        }

        log.info("库存扣减成功: specId={}, qty={}", specId, qty);
    }

    /** 取消/拒单时释放库存 */
    private void restoreStock(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            String key = "product:stock:" + item.getSpecId();
            redisTemplate.opsForValue().increment(key, item.getQuantity());
            log.info("库存释放: specId={}, qty={}", item.getSpecId(), item.getQuantity());
        }
    }

    // ──────────────────────────────────────
    // 内部方法 — 校验
    // ──────────────────────────────────────

    /** 校验商家营业时间（支持跨天，如 18:00 ~ 02:00） */
    private void validateBusinessHours(Merchant merchant) {
        if (merchant.getBusinessHoursStart() == null || merchant.getBusinessHoursEnd() == null) {
            return; // 未设置营业时间则跳过
        }
        LocalTime now = LocalTime.now();
        LocalTime start = merchant.getBusinessHoursStart();
        LocalTime end = merchant.getBusinessHoursEnd();

        boolean withinHours;
        if (end.isAfter(start)) {
            // 同天内：18:00 ~ 02:00
            withinHours = now.isAfter(start) && now.isBefore(end);
        } else {
            // 跨天：20:00 ~ 03:00
            withinHours = now.isAfter(start) || now.isBefore(end);
        }

        if (!withinHours) {
            throw new BizException(ErrorCode.NOT_IN_BUSINESS_HOURS,
                    String.format(MessageConst.ORDER_NOT_IN_BUSINESS_HOURS_MSG, start, end));
        }
    }

    private void assertStatus(Order order, OrderStatus expected, String msg) {
        if (order.getStatus() != expected) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, msg);
        }
    }

    private Order findOwnOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, MessageConst.ORDER_NOT_FOUND_MSG);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, MessageConst.ORDER_FORBIDDEN_MSG);
        }
        return order;
    }

    private Order findMerchantOrder(Long orderId, Long merchantId, Long userId) {
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || !merchant.getId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, MessageConst.NOT_MERCHANT_OWNER_SHORT_MSG);
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getMerchantId().equals(merchantId)) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, MessageConst.ORDER_NOT_FOUND_MSG);
        }
        return order;
    }

    // ──────────────────────────────────────
    // 内部方法 — 工具
    // ──────────────────────────────────────

    /**
     * 生成订单号：`NSP + yyyyMMdd + 当天 6 位自增序号`（如 NSP20260805000001）
     *
     * <p>序号由 Redis 原子自增（key `order:no:{yyyyMMdd}`，按天分桶）生成：
     * 下单即通过 INCR 预占一个序号，并发下多名用户也不会拿到重复订单号，
     * 同一天最多 999999 单；key 设置 2 天 TTL 防止无限堆积。
     * Redis 不可用时降级为「时间戳 + 4 位随机数」（极小概率冲突，由 uk_order_no 兜底）。
     */
    private String generateOrderNo() {
        String day = LocalDateTime.now().format(ORDER_NO_DAY);
        String key = "order:no:" + day;
        try {
            Long seq = redisTemplate.opsForValue().increment(key);
            if (seq != null && seq > 0) {
                redisTemplate.expire(key, Duration.ofDays(ORDER_NO_KEY_TTL_DAYS));
                return "NSP" + day + String.format("%06d", seq);
            }
        } catch (Exception e) {
            log.warn("Redis 订单号自增异常，降级时间戳随机: key={}", key, e);
        }
        return "NSP" + LocalDateTime.now().format(ORDER_NO_DATE) + String.format("%04d", new Random().nextInt(10000));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("地址快照 JSON 序列化失败", e);
            return "{}";
        }
    }

    private void saveStatusLog(Long orderId, String from, String to, String operator, String reason) {
        OrderStatusLog logEntry = new OrderStatusLog();
        logEntry.setOrderId(orderId);
        logEntry.setFromStatus(from);
        logEntry.setToStatus(to);
        logEntry.setOperator(operator);
        logEntry.setReason(reason);
        // 显式设置时间：created_at 列默认 CURRENT_TIMESTAMP（见 V1__init.sql），
        // 但业务上需要「事件发生时刻」与写库动作解耦，避免依赖列默认值
        logEntry.setCreatedAt(LocalDateTime.now());
        orderStatusLogMapper.insert(logEntry);
    }

    private PageVO<OrderListVO> toPageVO(Page<Order> result) {
        List<Order> records = result.getRecords();

        // 批量加载订单明细（一次 IN 查询，避免逐单 N+1）
        Map<Long, List<OrderItem>> itemMap = records.isEmpty() ? Map.of()
                : orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                        .in(OrderItem::getOrderId, records.stream().map(Order::getId).toList()))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderListVO> list = records.stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            BeanUtil.copyProperties(o, vo);
            vo.setStatus(o.getStatus().getCode());
            // 查询商家名称
            Merchant m = merchantMapper.selectById(o.getMerchantId());
            vo.setMerchantName(m != null ? m.getName() : "");
            // 填充商品明细（商家端订单卡片直接渲染）
            vo.setItems(itemMap.getOrDefault(o.getId(), List.of()).stream().map(oi -> {
                OrderItemVO iv = new OrderItemVO();
                BeanUtil.copyProperties(oi, iv);
                return iv;
            }).toList());
            return vo;
        }).toList();
        return new PageVO<>(result.getTotal(), list);
    }

    private OrderVO toVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtil.copyProperties(order, vo);
        vo.setStatus(order.getStatus().getCode());
        if (order.getPayChannel() != null) {
            vo.setPayChannel(order.getPayChannel().getCode());
        }

        // 商家名称
        Merchant merchant = merchantMapper.selectById(order.getMerchantId());
        vo.setMerchantName(merchant != null ? merchant.getName() : "");

        // 订单明细
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        vo.setItems(items.stream().map(oi -> {
            OrderItemVO iv = new OrderItemVO();
            BeanUtil.copyProperties(oi, iv);
            // 补充商品图片
            Product p = productMapper.selectById(oi.getProductId());
            if (p != null) iv.setImageUrl(p.getImageUrl());
            return iv;
        }).toList());

        // 状态日志
        List<OrderStatusLog> logs = orderStatusLogMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, order.getId())
                        .orderByAsc(OrderStatusLog::getCreatedAt));
        vo.setStatusLogs(logs.stream().map(l -> {
            OrderStatusLogVO lv = new OrderStatusLogVO();
            BeanUtil.copyProperties(l, lv);
            return lv;
        }).toList());

        return vo;
    }

}
