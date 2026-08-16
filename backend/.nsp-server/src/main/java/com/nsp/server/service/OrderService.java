package com.nsp.server.service;

import com.nsp.pojo.dto.OrderCreateDTO;
import com.nsp.pojo.vo.AdminOrderVO;
import com.nsp.pojo.vo.MerchantDashboardVO;
import com.nsp.pojo.vo.OrderListVO;
import com.nsp.pojo.vo.OrderVO;
import com.nsp.pojo.vo.PageVO;

/**
 * 订单业务接口 — 下单、状态流转、查询
 */
public interface OrderService {

    /** 用户下单 */
    OrderVO createOrder(Long userId, OrderCreateDTO dto);

    /** 用户查询自己的订单列表 */
    PageVO<OrderListVO> getUserOrders(Long userId, int page, int size);

    /** 商家查询自己店铺的订单列表（支持状态筛选与下单日期范围，非法状态值忽略） */
    PageVO<OrderListVO> getMerchantOrders(Long merchantId, Long userId, int page, int size,
                                          String status, String dateStart, String dateEnd);

    /** 商家订单状态分布统计（tab 角标）：status → 数量；可选下单日期范围（含边界当天） */
    java.util.Map<String, Long> getMerchantOrderStats(Long merchantId, Long userId,
                                                      String dateStart, String dateEnd);

    /** 商家工作台首页统计（今日订单/营业额/待接单/在售商品/今日状态分布） */
    MerchantDashboardVO getMerchantDashboard(Long merchantId, Long userId);

    /** 管理员查询全部订单列表（支持订单号/用户名/商家名关键词、状态、支付渠道、下单日期范围筛选） */
    PageVO<AdminOrderVO> getAdminOrders(int page, int size, String keyword, String status,
                                        String payChannel, String dateStart, String dateEnd);

    /** 查询订单详情（用户查自己的、商家查自己店的、管理员查所有） */
    OrderVO getOrderDetail(Long orderId, Long userId, String role);

    /** 用户取消订单（仅 PENDING_PAY 状态可取消） */
    void cancelOrder(Long orderId, Long userId);

    /** 商家接单 */
    void acceptOrder(Long orderId, Long merchantId, Long userId);

    /** 商家开始制作 */
    void startCooking(Long orderId, Long merchantId, Long userId);

    /** 商家完成制作 */
    void completeOrder(Long orderId, Long merchantId, Long userId);

    /** 商家拒单（已支付后拒单需触发退款） */
    void rejectOrder(Long orderId, Long merchantId, Long userId, String reason);

    /** 超时自动取消（PENDING_PAY 未支付 / PAID 商家未接单，已支付则 mock 退款） */
    void timeoutCancel(Long orderId, String reason);

    /** 超时自动完成（COOKING 超过时限视为已送达，自动转 COMPLETED） */
    void timeoutComplete(Long orderId);

    /** 批量删除订单（事务内级联删除订单明细/状态日志/支付记录，不存在/已删除的跳过） */
    com.nsp.pojo.vo.BatchResultVO batchDelete(java.util.List<Long> ids);

    /**
     * 管理员退款 — 可退款状态：PAID / ACCEPTED / COOKING；REFUNDING（上次失败）可重试。
     * 成功：订单 REFUNDING → 渠道退款 → REFUNDED；失败：保持 REFUNDING，抛 5007 可重试。
     */
    OrderVO refundOrder(Long orderId);

    /** 管理员批量退款 — 逐条执行 refundOrder，不可退（未支付/已完成/已退款/不存在）的跳过并汇总原因 */
    com.nsp.pojo.vo.BatchResultVO batchRefund(java.util.List<Long> ids);

}
