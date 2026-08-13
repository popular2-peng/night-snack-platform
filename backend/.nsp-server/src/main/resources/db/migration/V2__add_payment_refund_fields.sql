-- ============================================================
-- V2 — payment_record 增加退款字段
-- 背景：接入支付宝沙箱真实退款后，退款请求号（out_refund_no）需持久化，
--       保证重试时复用同一请求号（支付宝侧天然幂等）；
--       refund_status 记录退款中间态（REFUNDING 失败后可重试），
--       主状态 status 仍维持 PENDING/SUCCESS/FAILED/REFUNDED。
-- ============================================================

ALTER TABLE `payment_record`
    ADD COLUMN `out_refund_no`  VARCHAR(64)     DEFAULT NULL COMMENT '退款请求号（商户侧生成，唯一）' AFTER `trade_no`,
    ADD COLUMN `refund_amount`  DECIMAL(10,2)   DEFAULT NULL COMMENT '退款金额' AFTER `amount`,
    ADD COLUMN `refund_status`  VARCHAR(20)     DEFAULT NULL COMMENT '退款状态：REFUNDING/REFUNDED（成功时与 status=REFUNDED 同步）' AFTER `status`,
    ADD COLUMN `refund_at`      DATETIME        DEFAULT NULL COMMENT '退款完成时间' AFTER `last_notify_at`;
