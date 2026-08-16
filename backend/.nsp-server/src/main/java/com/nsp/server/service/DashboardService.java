package com.nsp.server.service;

import com.nsp.pojo.vo.AdminDashboardVO;
import com.nsp.pojo.vo.AdminRecentOrderVO;
import com.nsp.pojo.vo.OrderStatusStatVO;
import com.nsp.pojo.vo.TrendPointVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 仪表盘业务接口 — 今日核心统计、近 N 日趋势、订单状态分布、最新订单
 */
public interface DashboardService {

    /** 首页核心统计（今日订单/营业额/新增用户/待审核 + 较昨日对比 + 更新时间） */
    AdminDashboardVO getStats();

    /** 近 N 日注册用户趋势（每日新增用户数） */
    List<TrendPointVO> getUserTrend(int days);

    /** 近 N 日商家入驻趋势（每日入驻申请数） */
    List<TrendPointVO> getApplicationTrend(int days);

    /** 订单状态分布（date 为空时全量，否则只统计该日） */
    List<OrderStatusStatVO> getOrderStatusDistribution(LocalDate date);

    /** 最新订单列表 */
    List<AdminRecentOrderVO> getRecentOrders(int limit);

}
