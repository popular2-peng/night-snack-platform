-- ============================================================
-- V1__init.sql — 夜宵外卖平台 数据库初始化
-- 所有表统一：小写+下划线 | BIGINT 自增主键 | DATETIME 默认 CURRENT_TIMESTAMP
-- ============================================================

-- ──────────────────────────────────────────────
-- 1. 用户表（平台管理员 + 商家 + 普通用户共用）
-- ──────────────────────────────────────────────
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名（登录用，唯一）',
    `password`    VARCHAR(200) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER / MERCHANT / ADMIN',
    `avatar_url`  VARCHAR(500) DEFAULT NULL            COMMENT '头像 OSS URL',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ──────────────────────────────────────────────
-- 2. 用户收货地址
-- ──────────────────────────────────────────────
CREATE TABLE `user_address` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户 ID',
    `receiver`    VARCHAR(50)  NOT NULL                COMMENT '收件人姓名',
    `phone`       VARCHAR(20)  NOT NULL                COMMENT '收件人手机号',
    `detail`      VARCHAR(500) NOT NULL                COMMENT '详细地址',
    `lat`         DECIMAL(10,7) NOT NULL               COMMENT '纬度',
    `lng`         DECIMAL(10,7) NOT NULL               COMMENT '经度',
    `is_default`  TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否默认地址',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址';

-- ──────────────────────────────────────────────
-- 3. 商家/店铺
-- ──────────────────────────────────────────────
CREATE TABLE `merchant` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`                 VARCHAR(100) NOT NULL                COMMENT '店铺名称',
    `owner_user_id`        BIGINT       NOT NULL                COMMENT '店主用户 ID',
    `phone`                VARCHAR(20)  DEFAULT NULL            COMMENT '店铺联系电话',
    `address`              VARCHAR(500) DEFAULT NULL            COMMENT '店铺地址',
    `lat`                  DECIMAL(10,7) DEFAULT NULL           COMMENT '纬度',
    `lng`                  DECIMAL(10,7) DEFAULT NULL           COMMENT '经度',
    `avatar_url`           VARCHAR(500) DEFAULT NULL            COMMENT '店铺头像 OSS URL',
    `status`               VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '营业状态：PENDING/OPEN/CLOSED/SUSPENDED',
    `business_hours_start` TIME         DEFAULT NULL            COMMENT '营业开始时间（如 18:00）',
    `business_hours_end`   TIME         DEFAULT NULL            COMMENT '营业结束时间（如 02:00）',
    `delivery_radius`      INT          DEFAULT 5000            COMMENT '配送半径（米）',
    `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner_user_id` (`owner_user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家/店铺';

-- ──────────────────────────────────────────────
-- 4. 商品品类
-- ──────────────────────────────────────────────
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `merchant_id` BIGINT      NOT NULL                COMMENT '商家 ID',
    `name`        VARCHAR(50) NOT NULL                COMMENT '品类名称',
    `sort`        INT         DEFAULT 0               COMMENT '排序（越小越前）',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品品类';

-- ──────────────────────────────────────────────
-- 5. 商品
-- ──────────────────────────────────────────────
CREATE TABLE `product` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `merchant_id` BIGINT        NOT NULL                COMMENT '商家 ID',
    `category_id` BIGINT        NOT NULL                COMMENT '品类 ID',
    `name`        VARCHAR(100)  NOT NULL                COMMENT '商品名称',
    `price`       DECIMAL(10,2) NOT NULL                COMMENT '基础价格',
    `image_url`   VARCHAR(500)  DEFAULT NULL            COMMENT '商品图片 OSS URL',
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'ON'   COMMENT '状态：ON(上架) / OFF(下架)',
    `description` TEXT          DEFAULT NULL            COMMENT '商品描述',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品';

-- ──────────────────────────────────────────────
-- 6. 商品规格
-- ──────────────────────────────────────────────
CREATE TABLE `product_spec` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id`  BIGINT        NOT NULL                COMMENT '商品 ID',
    `name`        VARCHAR(50)   NOT NULL                COMMENT '规格名称（如"大杯""加珍珠"）',
    `price_delta` DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '价格增量（可负）',
    `stock`       INT           NOT NULL DEFAULT 0      COMMENT '库存数量',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格';

-- ──────────────────────────────────────────────
-- 7. 订单
-- ──────────────────────────────────────────────
CREATE TABLE `order` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`         VARCHAR(32)   NOT NULL                COMMENT '订单号（业务唯一）',
    `user_id`          BIGINT        NOT NULL                COMMENT '用户 ID',
    `merchant_id`      BIGINT        NOT NULL                COMMENT '商家 ID',
    `status`           VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态',
    `total_amount`     DECIMAL(10,2) NOT NULL                COMMENT '实付总金额',
    `address_snapshot` TEXT          DEFAULT NULL            COMMENT '收货地址快照（JSON）',
    `pay_channel`      VARCHAR(20)   DEFAULT NULL            COMMENT '支付渠道：ALIPAY / WECHAT_MOCK',
    `pay_time`         DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_merchant_id` (`merchant_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单';

-- ──────────────────────────────────────────────
-- 8. 订单明细
-- ──────────────────────────────────────────────
CREATE TABLE `order_item` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`       BIGINT        NOT NULL                COMMENT '订单 ID',
    `product_id`     BIGINT        NOT NULL                COMMENT '商品 ID',
    `spec_id`        BIGINT        DEFAULT NULL            COMMENT '规格 ID',
    `name_snapshot`  VARCHAR(200)  NOT NULL                COMMENT '商品+规格名称快照',
    `price_snapshot` DECIMAL(10,2) NOT NULL                COMMENT '下单时单价',
    `quantity`       INT           NOT NULL DEFAULT 1      COMMENT '购买数量',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细';

-- ──────────────────────────────────────────────
-- 9. 订单状态流转日志（审计）
-- ──────────────────────────────────────────────
CREATE TABLE `order_status_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT       NOT NULL                COMMENT '订单 ID',
    `from_status` VARCHAR(20)  DEFAULT NULL            COMMENT '变更前状态',
    `to_status`   VARCHAR(20)  NOT NULL                COMMENT '变更后状态',
    `operator`    VARCHAR(50)  DEFAULT NULL            COMMENT '操作人',
    `reason`      VARCHAR(500) DEFAULT NULL            COMMENT '变更原因',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态流转日志';

-- ──────────────────────────────────────────────
-- 10. 支付记录
-- ──────────────────────────────────────────────
CREATE TABLE `payment_record` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`        VARCHAR(32)   NOT NULL                COMMENT '订单号',
    `channel`         VARCHAR(20)   NOT NULL                COMMENT '支付渠道：ALIPAY / WECHAT_MOCK',
    `trade_no`        VARCHAR(100)  DEFAULT NULL            COMMENT '第三方交易号',
    `amount`          DECIMAL(10,2) NOT NULL                COMMENT '支付金额',
    `status`          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING/SUCCESS/FAILED/REFUNDED',
    `notify_count`    INT           DEFAULT 0               COMMENT '回调通知次数',
    `last_notify_at`  DATETIME      DEFAULT NULL            COMMENT '最后一次回调时间',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no_channel` (`order_no`, `channel`),
    KEY `idx_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录';

-- ──────────────────────────────────────────────
-- 11. 已上传文件
-- ──────────────────────────────────────────────
CREATE TABLE `uploaded_file` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type`    VARCHAR(30)  NOT NULL                COMMENT '业务类型：AVATAR/SHOP_LOGO/PRODUCT_IMAGE',
    `target_id`   BIGINT       DEFAULT NULL            COMMENT '关联业务对象 ID',
    `oss_key`     VARCHAR(500) NOT NULL                COMMENT 'OSS 对象 Key',
    `url`         VARCHAR(500) NOT NULL                COMMENT '文件访问 URL',
    `uploader_id` BIGINT       NOT NULL                COMMENT '上传者用户 ID',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_biz_type_target` (`biz_type`, `target_id`),
    KEY `idx_uploader_id` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已上传文件';

-- ──────────────────────────────────────────────
-- 12. 商家入驻申请
-- ──────────────────────────────────────────────
CREATE TABLE `merchant_application` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `applicant_user_id` BIGINT       NOT NULL                COMMENT '申请人用户 ID',
    `shop_name`         VARCHAR(100) NOT NULL                COMMENT '申请店铺名称',
    `reason`            TEXT         DEFAULT NULL            COMMENT '申请理由',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
    `reviewer_id`       BIGINT       DEFAULT NULL            COMMENT '审核人 ID（管理员）',
    `reviewed_at`       DATETIME     DEFAULT NULL            COMMENT '审核时间',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_applicant_user_id` (`applicant_user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家入驻申请';
