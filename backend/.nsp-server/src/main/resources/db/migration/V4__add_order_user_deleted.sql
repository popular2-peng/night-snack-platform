-- 用户端订单「删除」（软删）：用户删除后 user_deleted=1，用户端列表查询对其不可见
ALTER TABLE `order`
    ADD COLUMN `user_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '用户删除标记：0-未删除 1-已删除（对用户不显示）' AFTER `pay_time`;
