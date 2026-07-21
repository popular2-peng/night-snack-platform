package com.nsp.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nsp.common.constant.ErrorCode;
import com.nsp.common.enums.OrderStatus;
import com.nsp.common.exception.BizException;
import com.nsp.pojo.dto.PayRequestDTO;
import com.nsp.pojo.entity.Order;
import com.nsp.pojo.entity.OrderItem;
import com.nsp.pojo.entity.PaymentRecord;
import com.nsp.pojo.entity.Product;
import com.nsp.pojo.vo.PayVO;
import com.nsp.server.mapper.OrderItemMapper;
import com.nsp.server.mapper.OrderMapper;
import com.nsp.server.mapper.PaymentRecordMapper;
import com.nsp.server.mapper.ProductMapper;
import com.nsp.server.service.AlipayService;
import com.nsp.server.service.PaymentService;
import com.nsp.server.service.WechatMockService;
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
    private final AlipayService alipayService;
    private final WechatMockService wechatMockService;

    @Override
    @Transactional
    public PayVO pay(Long userId, PayRequestDTO dto) {
        // 查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, dto.getOrderNo()));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "无权操作该订单");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAY) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR,
                    "当前状态不允许支付: " + order.getStatus().getDesc());
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
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的支付渠道: " + dto.getChannel());
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
            throw new BizException(ErrorCode.PAY_SIGN_ERROR, "支付宝回调验签失败");
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
            throw new BizException(ErrorCode.PAY_ORDER_NOT_FOUND, "未找到支付记录");
        }
        return PayVO.builder()
                .orderNo(orderNo)
                .channel(record.getChannel())
                .amount(record.getAmount())
                .status(record.getStatus())
                .build();
    }

    // ──────────────────────────────────────
    // 内部方法
    // ──────────────────────────────────────

    /** 支付成功后更新订单状态 */
    private void markOrderPaid(Order order, String channel, String tradeNo) {
        order.setStatus(OrderStatus.PAID);
        order.setPayChannel(com.nsp.common.enums.PayChannel.fromCode(channel));
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单已支付: orderNo={}, channel={}, tradeNo={}", order.getOrderNo(), channel, tradeNo);
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
