package com.nsp.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.constant.MessageConst;
import com.nsp.common.enums.OrderStatus;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.PayRequestDTO;
import com.nsp.pojo.entity.Order;
import com.nsp.pojo.entity.OrderItem;
import com.nsp.pojo.entity.OrderStatusLog;
import com.nsp.pojo.entity.PaymentRecord;
import com.nsp.pojo.entity.Product;
import com.nsp.pojo.vo.PayVO;
import com.nsp.server.config.JacksonConfig;
import com.nsp.server.mapper.OrderItemMapper;
import com.nsp.server.mapper.OrderMapper;
import com.nsp.server.mapper.OrderStatusLogMapper;
import com.nsp.server.mapper.PaymentRecordMapper;
import com.nsp.server.mapper.ProductMapper;
import com.nsp.server.service.AlipayService;
import com.nsp.server.service.PaymentService;
import com.nsp.server.service.WechatMockService;
import com.nsp.server.ws.MerchantWsSessionManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付业务实现
 *
 * <p>⚡ 支付回调必须验签 + 幂等（查 payment_record 的 order_no+channel 唯一键）
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ProductMapper productMapper;
    private final OrderStatusLogMapper orderStatusLogMapper;
    private final AlipayService alipayService;
    private final WechatMockService wechatMockService;
    private final MerchantWsSessionManager wsSessionManager;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PayVO pay(Long userId, PayRequestDTO dto) {
        // 查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, dto.getOrderNo()));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, MessageConst.ORDER_NOT_FOUND_MSG);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, MessageConst.ORDER_FORBIDDEN_MSG);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR,
                    String.format(MessageConst.ORDER_STATUS_PAY_MSG, order.getStatus().getDesc()));
        }

        // 按渠道分发
        String channelResult;
        if ("ALIPAY".equalsIgnoreCase(dto.getChannel())) {
            // 组装商品标题
            String subject = buildSubject(order.getId());
            String amountYuan = order.getTotalAmount()
                    .setScale(2, RoundingMode.HALF_UP).toString();
            channelResult = alipayService.precreate(dto.getOrderNo(), subject, amountYuan);
        } else if ("WECHAT_MOCK".equalsIgnoreCase(dto.getChannel())) {
            channelResult = wechatMockService.mockPay(dto.getOrderNo());
            // Mock 直接标记为已支付
            markOrderPaid(order, dto.getChannel(), "MOCK_" + dto.getOrderNo());
        } else {
            throw new BizException(ErrorCode.PARAM_ERROR, String.format(MessageConst.PARAM_PAY_CHANNEL_UNSUPPORTED_MSG, dto.getChannel()));
        }

        // 记录支付请求（支付宝渠道：状态 PENDING 等待回调）
        PaymentRecord record = new PaymentRecord();
        record.setOrderNo(dto.getOrderNo());
        record.setChannel(dto.getChannel());
        record.setAmount(order.getTotalAmount());
        record.setStatus("WECHAT_MOCK".equalsIgnoreCase(dto.getChannel()) ? "SUCCESS" : "PENDING");
        record.setNotifyCount(0);
        paymentRecordMapper.insert(record);

        log.info("发起支付: orderNo={}, channel={}, result={}",
                dto.getOrderNo(), dto.getChannel(), channelResult);

        return PayVO.builder()
                .orderNo(dto.getOrderNo())
                .channel(dto.getChannel())
                .amount(order.getTotalAmount())
                .status(record.getStatus())
                .qrCode("ALIPAY".equalsIgnoreCase(dto.getChannel()) ? channelResult : null)
                .build();
    }

    @Override
    @Transactional
    public void handleAlipayCallback(Map<String, String> params) {
        // 1. 验签
        if (!alipayService.verifySign(params)) {
            log.error("支付宝回调验签失败: params={}", params);
            throw new BizException(ErrorCode.PAY_SIGN_ERROR, MessageConst.PAY_ALIPAY_SIGN_ERROR_MSG);
        }

        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String totalAmount = params.get("total_amount");
        String tradeStatus = params.get("trade_status");

        // 2. 幂等检查
        List<PaymentRecord> records = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderNo, orderNo)
                        .eq(PaymentRecord::getChannel, "ALIPAY"));
        if (!records.isEmpty() && "SUCCESS".equals(records.get(0).getStatus())) {
            log.info("支付宝回调重复通知（幂等跳过）: orderNo={}", orderNo);
            return;
        }

        // 3. 只处理 TRADE_SUCCESS
        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            log.info("支付宝回调非成功状态: orderNo={}, tradeStatus={}", orderNo, tradeStatus);
            return;
        }

        // 4. 更新支付记录
        PaymentRecord record = records.isEmpty() ? new PaymentRecord() : records.get(0);
        record.setOrderNo(orderNo);
        record.setChannel("ALIPAY");
        record.setTradeNo(tradeNo);
        record.setAmount(new BigDecimal(totalAmount));
        record.setStatus("SUCCESS");
        record.setNotifyCount(records.isEmpty() ? 1 : record.getNotifyCount() + 1);
        record.setLastNotifyAt(LocalDateTime.now());
        if (records.isEmpty()) {
            paymentRecordMapper.insert(record);
        } else {
            paymentRecordMapper.updateById(record);
        }

        // 5. 更新订单状态
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (order != null) {
            markOrderPaid(order, "ALIPAY", tradeNo);
        }

        log.info("支付宝回调处理完成: orderNo={}, tradeNo={}", orderNo, tradeNo);
    }

    @Override
    public PayVO queryStatus(String orderNo) {
        PaymentRecord record = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderNo, orderNo)
                        .orderByDesc(PaymentRecord::getCreatedAt)
                        .last("LIMIT 1"));
        if (record == null) {
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND, MessageConst.PAY_RECORD_NOT_FOUND_MSG);
        }
        return PayVO.builder()
                .orderNo(orderNo)
                .channel(record.getChannel())
                .amount(record.getAmount())
                .status(record.getStatus())
                .build();
    }

    @Override
    public void refund(String orderNo) {
        PaymentRecord record = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderNo, orderNo)
                        .last("LIMIT 1"));
        if (record == null) {
            throw new BizException(ErrorCode.REFUND_RECORD_NOT_FOUND, MessageConst.REFUND_RECORD_NOT_FOUND_MSG);
        }
        // 已退款 → 幂等直接返回
        if ("REFUNDED".equals(record.getStatus())) {
            log.info("订单已退款（幂等跳过）: orderNo={}", orderNo);
            return;
        }
        // 未成功支付的记录不可退款
        if (!"SUCCESS".equals(record.getStatus())) {
            throw new BizException(ErrorCode.REFUND_RECORD_NOT_FOUND, MessageConst.REFUND_RECORD_NOT_FOUND_MSG);
        }

        String channel = record.getChannel();
        BigDecimal amount = record.getAmount();

        // 微信 mock 通道：直接成功（保留渠道抽象，正式渠道替换实现）
        if ("WECHAT_MOCK".equalsIgnoreCase(channel)) {
            wechatMockService.mockRefund(orderNo);
            markRefundSuccess(record, amount);
            return;
        }

        if (!"ALIPAY".equalsIgnoreCase(channel)) {
            throw new BizException(ErrorCode.PARAM_ERROR,
                    String.format(MessageConst.PARAM_PAY_CHANNEL_UNSUPPORTED_MSG, channel));
        }

        // 生成/复用退款请求号并置 REFUNDING —— 短事务落库，不包住外部渠道调用
        if (record.getOutRefundNo() == null) {
            record.setOutRefundNo(generateOutRefundNo(orderNo));
        }
        record.setRefundStatus("REFUNDING");
        paymentRecordMapper.updateById(record);
        log.info("发起退款: orderNo={}, outRefundNo={}", orderNo, record.getOutRefundNo());

        try {
            alipayService.refund(orderNo, record.getOutRefundNo(),
                    amount.setScale(2, RoundingMode.HALF_UP).toString());
        } catch (BizException e) {
            // 沙箱无真实交易（未在沙箱实际支付）→ 降级本地标记退款，保证演示链路完整
            if (e.getCode() == ErrorCode.REFUND_TRADE_NOT_EXIST) {
                log.warn("支付宝退款降级为本地标记: orderNo={}", orderNo);
                markRefundSuccess(record, amount);
                return;
            }
            // 其他失败：保持 REFUNDING，可重试
            throw e;
        }
        markRefundSuccess(record, amount);
    }

    // ──────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────

    /** 退款成功落库：主状态 + 退款中间态 + 退款金额 + 退款时间 */
    private void markRefundSuccess(PaymentRecord record, BigDecimal amount) {
        record.setStatus("REFUNDED");
        record.setRefundStatus("REFUNDED");
        record.setRefundAmount(amount);
        record.setRefundAt(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
        log.info("退款完成: orderNo={}, amount={}", record.getOrderNo(), amount);
    }

    /** 生成退款请求号：RF + 订单号 + 毫秒时间戳（重试时复用已持久化的值，保证渠道侧幂等） */
    private String generateOutRefundNo(String orderNo) {
        return "RF" + orderNo + System.currentTimeMillis();
    }

    /** 支付成功后更新订单状态（WECHAT_MOCK 直付与支付宝回调统一入口） */
    private void markOrderPaid(Order order, String channel, String tradeNo) {
        order.setStatus(OrderStatus.PAID);
        order.setPayChannel(com.nsp.common.enums.PayChannel.fromCode(channel));
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 记录状态流转日志（显式设置时间，避免列随机默认值导致时间乱序）
        OrderStatusLog statusLog = new OrderStatusLog();
        statusLog.setOrderId(order.getId());
        statusLog.setFromStatus(OrderStatus.PENDING_PAY.getCode());
        statusLog.setToStatus(OrderStatus.PAID.getCode());
        statusLog.setOperator("system");
        statusLog.setReason("支付回调成功");
        statusLog.setCreatedAt(LocalDateTime.now());
        orderStatusLogMapper.insert(statusLog);

        log.info("订单已支付: orderNo={}, channel={}, tradeNo={}", order.getOrderNo(), channel, tradeNo);

        // WebSocket 实时推送：通知商家有新订单（推送失败不影响支付主流程）
        pushNewOrderNotify(order);
    }

    /** WebSocket 推送 NEW_ORDER 消息给对应商家（尽力而为，失败仅告警） */
    private void pushNewOrderNotify(Order order) {
        try {
            // createdAt 统一 ISO-8601（与 REST 接口时间格式一致，不带纳秒尾巴）
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "NEW_ORDER",
                    "data", Map.of(
                            "orderId", order.getId(),
                            "orderNo", order.getOrderNo(),
                            "totalAmount", order.getTotalAmount(),
                            "createdAt", JacksonConfig.LOCAL_DATE_TIME_FORMATTER.format(LocalDateTime.now())
                    )));
            wsSessionManager.sendToMerchant(order.getMerchantId(), payload);
        } catch (Exception e) {
            log.warn("新订单 WS 推送失败（不影响支付）: orderNo={}, err={}", order.getOrderNo(), e.getMessage());
        }
    }

    /** 组装商品描述 */
    private String buildSubject(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        if (items.isEmpty()) return "夜宵外卖订单";
        OrderItem first = items.get(0);
        int remain = items.size() - 1;
        String name = first.getNameSnapshot();
        if (remain > 0) name += " 等" + items.size() + "件";
        return name.length() > 50 ? name.substring(0, 47) + "..." : name;
    }

}
