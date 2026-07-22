-- ============================================================
-- V3__user_status.sql — 用户表新增 status 字段，支持管理员封禁
-- ============================================================
ALTER TABLE `user`
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    COMMENT '状态：ACTIVE(正常) / DISABLED(已封禁)'
    AFTER `role`;
