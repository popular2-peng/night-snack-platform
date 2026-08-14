-- ============================================================
-- V3 — user_address 坐标改为可空
-- 背景：移动端 MVP 暂不支持地图选点，地址创建/编辑可不传经纬度；
--       下单时坐标为空则距离校验按 0 处理（配送范围校验降级），
--       后续接入地图选点后恢复严格校验。
-- ============================================================

ALTER TABLE `user_address`
    MODIFY COLUMN `lat` DECIMAL(10,7) DEFAULT NULL COMMENT '纬度（可选，移动端地图选点后续接入）',
    MODIFY COLUMN `lng` DECIMAL(10,7) DEFAULT NULL COMMENT '经度（可选）';
