package com.nsp.server.service;

import com.nsp.pojo.dto.OrderCreateDTO;
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

}
