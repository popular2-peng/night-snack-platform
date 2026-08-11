-- ============================================================
-- V3: 修复订单号与创建时间不一致（P0）
-- ============================================================
-- 背景：种子订单号是 V1 硬编码的造数日期（如 NSP20260727015），
--       而 created_at 之后被随机化（过去 2 年内任意时刻），
--       导致订单号日期与订单创建时间脱节（如 NSP20260727xxx 的
--       创建时间却是 2026-08-05）。
-- 修复：订单号按创建时间重新生成 —— `NSP + yyyyMMdd + 当天 6 位递增序号`，
--       与运行期 Redis 自增生成规则（见 OrderServiceImpl.generateOrderNo）一致。
-- 顺序：先同步 payment_record.order_no（用旧号关联映射），再改 order 表。
-- ============================================================

-- 1. 计算新旧订单号映射（临时表）
DROP TEMPORARY TABLE IF EXISTS tmp_order_no_map;
CREATE TEMPORARY TABLE tmp_order_no_map AS
SELECT o.id,
       o.order_no AS old_no,
       CONCAT('NSP', DATE_FORMAT(o.created_at, '%Y%m%d'),
              LPAD(ROW_NUMBER() OVER (PARTITION BY DATE_FORMAT(o.created_at, '%Y%m%d')
                                      ORDER BY o.created_at, o.id), 6, '0')) AS new_no
FROM `order` o;

-- 2. 同步支付记录（按旧订单号关联）
UPDATE payment_record p
JOIN tmp_order_no_map t ON p.order_no = t.old_no
SET p.order_no = t.new_no;

-- 3. 重排订单号（唯一键冲突前先同步支付记录，故此处直接 UPDATE）
UPDATE `order` o
JOIN tmp_order_no_map t ON o.id = t.id
SET o.order_no = t.new_no;

DROP TEMPORARY TABLE tmp_order_no_map;
