-- ============================================================
-- V2: 商家表 & 入驻申请表 新增字段
-- 来源：docs/商家管理与入驻审核修改建议.md 第二节
-- ============================================================

-- 2.1 merchant 表新增字段
ALTER TABLE merchant ADD COLUMN logo_url VARCHAR(500) DEFAULT NULL COMMENT '店铺LOGO/头像 OSS URL';
ALTER TABLE merchant ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '主营品类ID(关联category表)';
ALTER TABLE merchant ADD COLUMN description TEXT DEFAULT NULL COMMENT '店铺简介/招牌描述';
ALTER TABLE merchant ADD COLUMN id_card_no VARCHAR(50) DEFAULT NULL COMMENT '店主身份证号(加密存储)';
ALTER TABLE merchant ADD COLUMN avg_rating DECIMAL(3,1) DEFAULT 0.0 COMMENT '店铺平均评分';
ALTER TABLE merchant ADD COLUMN total_orders INT DEFAULT 0 COMMENT '历史总订单数';
ALTER TABLE merchant ADD COLUMN today_orders INT DEFAULT 0 COMMENT '今日订单数(缓存)';
ALTER TABLE merchant ADD COLUMN today_revenue DECIMAL(10,2) DEFAULT 0.00 COMMENT '今日营收(缓存)';
ALTER TABLE merchant ADD COLUMN banned_reason VARCHAR(500) DEFAULT NULL COMMENT '封禁原因(管理员填写)';
ALTER TABLE merchant ADD COLUMN banned_at DATETIME DEFAULT NULL COMMENT '封禁时间';
ALTER TABLE merchant ADD COLUMN banned_by BIGINT DEFAULT NULL COMMENT '封禁操作人(关联user.id)';
ALTER TABLE merchant ADD COLUMN last_login_at DATETIME DEFAULT NULL COMMENT '商家最后登录时间';
ALTER TABLE merchant ADD COLUMN notice TEXT DEFAULT NULL COMMENT '店铺公告/营业备注';

-- 2.2 merchant_application 表新增字段
ALTER TABLE merchant_application ADD COLUMN logo_url VARCHAR(500) DEFAULT NULL COMMENT '申请时上传的店铺LOGO';
ALTER TABLE merchant_application ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '申请的主营品类';
ALTER TABLE merchant_application ADD COLUMN description TEXT DEFAULT NULL COMMENT '店铺简介';
ALTER TABLE merchant_application ADD COLUMN id_card_no VARCHAR(50) DEFAULT NULL COMMENT '店主身份证号(加密)';
ALTER TABLE merchant_application ADD COLUMN review_note TEXT DEFAULT NULL COMMENT '审核备注(通过/拒绝时填写)';
ALTER TABLE merchant_application ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因(展示给商家看)';
