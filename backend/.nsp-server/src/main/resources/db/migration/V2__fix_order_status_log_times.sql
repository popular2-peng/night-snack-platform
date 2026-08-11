-- ============================================================
-- V2: 修复订单状态流转日志时间乱序（P0）
-- ============================================================
-- 背景：V1 中 `order_status_log.created_at` 使用随机默认值
--       （过去 2 年内任意时刻），种子日志未显式指定时间，
--       导致同一订单的状态流转时间完全乱序
--       （如支付早于下单、接单晚于制作完成、下单晚于一切）。
-- 修复：以订单创建时间为基准，按状态机顺序重排每条日志时间：
--   PENDING_PAY → 下单时刻（created_at）
--   PAID        → 订单支付时间（pay_time，V1 已保证在 created 后 5~65 分钟）
--   ACCEPTED    → created + 100 分钟
--   COOKING     → created + 160 分钟
--   COMPLETED   → created + 220 分钟
--   CANCELLED   → created + 260 分钟
--   REFUNDING   → created + 280 分钟
--   REFUNDED    → created + 310 分钟
-- 并叠加 0~4 分钟随机抖动，模拟真实操作间隔（同一订单跨度收敛到几小时内）。
-- ============================================================

UPDATE `order_status_log` l
JOIN `order` o ON l.order_id = o.id
SET l.created_at = CASE l.to_status
    WHEN 'PENDING_PAY' THEN o.created_at
    WHEN 'PAID' THEN COALESCE(o.pay_time, o.created_at + INTERVAL 30 MINUTE)
    WHEN 'ACCEPTED'  THEN o.created_at + INTERVAL 100 MINUTE + INTERVAL FLOOR(RAND() * 5) MINUTE
    WHEN 'COOKING'   THEN o.created_at + INTERVAL 160 MINUTE + INTERVAL FLOOR(RAND() * 5) MINUTE
    WHEN 'COMPLETED' THEN o.created_at + INTERVAL 220 MINUTE + INTERVAL FLOOR(RAND() * 5) MINUTE
    WHEN 'CANCELLED' THEN o.created_at + INTERVAL 260 MINUTE + INTERVAL FLOOR(RAND() * 5) MINUTE
    WHEN 'REFUNDING' THEN o.created_at + INTERVAL 280 MINUTE + INTERVAL FLOOR(RAND() * 5) MINUTE
    WHEN 'REFUNDED'  THEN o.created_at + INTERVAL 310 MINUTE + INTERVAL FLOOR(RAND() * 5) MINUTE
    ELSE o.created_at + INTERVAL 60 MINUTE
END;
