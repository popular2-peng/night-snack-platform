package com.nsp.server.service;

import com.nsp.pojo.vo.AdminDashboardVO;
import com.nsp.pojo.vo.AdminRecentOrderVO;
import com.nsp.pojo.vo.OrderStatusStatVO;

import java.util.List;

/**
 * 仪表盘业务接口 — 首页统计卡片、订单分布、最新订单
 */
public interface DashboardService {

    /** 首页核心统计（用户数/商家数/订单数/待审核数） */
    AdminDashboardVO getStats();

    /** 订单状态分布（饼图/柱状图数据） */
    List<OrderStatusStatVO> getOrderStatusDistribution();

    /** 最新订单列表 */
    List<AdminRecentOrderVO> getRecentOrders(int limit);

}
