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
    private final AddressMapper addressMapper;
    private final StringRedisTemplate redisTemplate;
    private final MapService mapService;
    private final CartService cartService;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ORDER_NO_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ──────────────────────────────────────
    // 下单
    // ──────────────────────────────────────

    @Override
    @Transactional
    public OrderVO createOrder(Long userId, OrderCreateDTO dto) {
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

        return toVO(order);
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
                        .orderByDesc(Order::getCreatedAt));

        return toPageVO(result);
    }

    @Override
    public PageVO<OrderListVO> getMerchantOrders(Long merchantId, Long userId, int page, int size) {
        // 校验 userId 是该商家店主
        Merchant merchant = merchantMapper.selectById(merchantId);
        if (merchant == null || !merchant.getId().equals(userId)) {
            throw new BizException(ErrorCode.NOT_MERCHANT_OWNER, MessageConst.NOT_MERCHANT_ORDER_VIEW_MSG);
        }

        Page<Order> result = orderMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getMerchantId, merchantId)
                        .orderByDesc(Order::getCreatedAt));

        return toPageVO(result);
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

        // 释放库存（退款由 PaymentService 处理）
        restoreStock(orderId);

        saveStatusLog(orderId, OrderStatus.PAID.getCode(),
                OrderStatus.CANCELLED.getCode(), "MERCHANT", "商家拒单: " + reason);
        log.info("商家拒单: orderId={}, orderNo={}, reason={}", orderId, order.getOrderNo(), reason);
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

    private String generateOrderNo() {
        String prefix = "NSP" + LocalDateTime.now().format(ORDER_NO_DATE);
        String suffix = String.format("%04d", new Random().nextInt(10000));
        return prefix + suffix;
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
        orderStatusLogMapper.insert(logEntry);
    }

    private PageVO<OrderListVO> toPageVO(Page<Order> result) {
        List<OrderListVO> list = result.getRecords().stream().map(o -> {
            OrderListVO vo = new OrderListVO();
            BeanUtil.copyProperties(o, vo);
            vo.setStatus(o.getStatus().getCode());
            // 查询商家名称
            Merchant m = merchantMapper.selectById(o.getMerchantId());
            vo.setMerchantName(m != null ? m.getName() : "");
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
