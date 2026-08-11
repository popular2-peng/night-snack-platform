package com.nsp.server.service;

import com.nsp.pojo.dto.OrderCreateDTO;
import com.nsp.pojo.vo.AdminOrderVO;
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

    /** 商家查询自己店铺的订单列表 */
    PageVO<OrderListVO> getMerchantOrders(Long merchantId, Long userId, int page, int size);

    /** 管理员查询全部订单列表（支持订单号/用户名/商家名关键词、状态、下单日期范围筛选） */
    PageVO<AdminOrderVO> getAdminOrders(int page, int size, String keyword, String status,
                                        String dateStart, String dateEnd);

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

}
