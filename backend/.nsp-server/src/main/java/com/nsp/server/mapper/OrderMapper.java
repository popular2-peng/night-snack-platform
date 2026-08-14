package com.nsp.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nsp.pojo.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 订单数据访问层
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 查询单个商家今日订单统计（订单数 + 营收）
     * 营收排除已取消/已退款订单
     */
    @Select("SELECT COUNT(*) AS order_count, " +
            "COALESCE(SUM(CASE WHEN status NOT IN ('CANCELLED', 'REFUNDED') THEN total_amount ELSE 0 END), 0) AS revenue " +
            "FROM `order` WHERE merchant_id = #{merchantId} AND DATE(created_at) = CURDATE()")
    Map<String, Object> selectTodayStats(@Param("merchantId") Long merchantId);

    /**
     * 批量查询多个商家今日订单统计（订单数 + 营收）
     * 返回每行含 merchant_id, order_count, revenue
     */
    @Select("<script>" +
            "SELECT merchant_id, COUNT(*) AS order_count, " +
            "COALESCE(SUM(CASE WHEN status NOT IN ('CANCELLED', 'REFUNDED') THEN total_amount ELSE 0 END), 0) AS revenue " +
            "FROM `order` WHERE merchant_id IN " +
            "<foreach item='id' collection='merchantIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            " AND DATE(created_at) = CURDATE() " +
            "GROUP BY merchant_id" +
            "</script>")
    List<Map<String, Object>> selectTodayStatsBatch(@Param("merchantIds") List<Long> merchantIds);

    /**
     * 批量查询用户订单统计（历史总订单数 + 历史总消费金额）
     * 消费金额排除已取消/已退款订单
     * 返回每行含 user_id, order_count, total_spent
     */
    @Select("<script>" +
            "SELECT user_id, COUNT(*) AS order_count, " +
            "COALESCE(SUM(CASE WHEN status NOT IN ('CANCELLED', 'REFUNDED') THEN total_amount ELSE 0 END), 0) AS total_spent " +
            "FROM `order` WHERE user_id IN " +
            "<foreach item='id' collection='userIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            " GROUP BY user_id" +
            "</script>")
    List<Map<String, Object>> selectUserOrderStats(@Param("userIds") List<Long> userIds);

    /**
     * 单商家订单状态分布统计（商家端订单管理 tab 角标用）
     * 返回每行含 status, cnt
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM `order` " +
            "WHERE merchant_id = #{merchantId} GROUP BY status")
    List<Map<String, Object>> selectStatusStats(@Param("merchantId") Long merchantId);

}
