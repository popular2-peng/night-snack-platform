-- ============================================================
-- V1__init.sql — 夜宵外卖平台 数据库初始化（含种子数据）
--
-- 架构变更：
--   原 user 表（role=USER/MERCHANT/ADMIN）拆分为三张独立表：
--     admin      — 平台管理员
--     user       — 普通用户（下单顾客）
--     merchant   — 商家（含登录凭据，自包含）
--
-- 数据库重建流程：
--   1. mysql -u root -p -e "DROP DATABASE IF EXISTS nsp_db; CREATE DATABASE nsp_db DEFAULT CHARSET utf8mb4;"
--   2. mysql -u root -p -e "DELETE FROM nsp_db.flyway_schema_history;"  （如 Flyway 校验失败）
--   3. 重启后端应用，Flyway 自动执行本脚本
-- ============================================================

-- ============================================================
-- 一、平台管理员表
-- ============================================================
CREATE TABLE `admin` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名（登录用，唯一）',
    `password`   VARCHAR(200) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `phone`      VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `avatar_url` VARCHAR(500) DEFAULT NULL            COMMENT '头像 OSS URL',
    `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE(正常) / DISABLED(已封禁)',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台管理员';

-- ============================================================
-- 二、普通用户表（下单顾客）
-- ============================================================
CREATE TABLE `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名（登录用，唯一）',
    `password`   VARCHAR(200) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `phone`      VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `avatar_url` VARCHAR(500) DEFAULT NULL            COMMENT '头像 OSS URL',
    `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE(正常) / DISABLED(已封禁)',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户（顾客）';

-- ============================================================
-- 三、用户收货地址
-- ============================================================
CREATE TABLE `user_address` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT        NOT NULL                COMMENT '用户 ID',
    `receiver`   VARCHAR(50)   NOT NULL                COMMENT '收件人姓名',
    `phone`      VARCHAR(20)   NOT NULL                COMMENT '收件人手机号',
    `detail`     VARCHAR(500)  NOT NULL                COMMENT '详细地址',
    `lat`        DECIMAL(10,7) NOT NULL                COMMENT '纬度',
    `lng`        DECIMAL(10,7) NOT NULL                COMMENT '经度',
    `is_default` TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否默认地址',
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址';

-- ============================================================
-- 四、商家/店铺（含登录凭据，自包含）
-- ============================================================
CREATE TABLE `merchant` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`             VARCHAR(50)   NOT NULL                COMMENT '商家登录用户名（唯一）',
    `password`             VARCHAR(200)  NOT NULL                COMMENT '登录密码（BCrypt 加密）',
    `name`                 VARCHAR(100)  NOT NULL                COMMENT '店铺名称',
    `phone`                VARCHAR(20)   DEFAULT NULL            COMMENT '店铺联系电话',
    `address`              VARCHAR(500)  DEFAULT NULL            COMMENT '店铺地址',
    `lat`                  DECIMAL(10,7) DEFAULT NULL            COMMENT '纬度',
    `lng`                  DECIMAL(10,7) DEFAULT NULL            COMMENT '经度',
    `avatar_url`           VARCHAR(500)  DEFAULT NULL            COMMENT '店铺头像 OSS URL',
    `status`               VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '营业状态：PENDING/OPEN/CLOSED/SUSPENDED',
    `business_hours_start` TIME          DEFAULT NULL            COMMENT '营业开始时间（如 18:00）',
    `business_hours_end`   TIME          DEFAULT NULL            COMMENT '营业结束时间（如 02:00）',
    `delivery_radius`      INT           DEFAULT 5000            COMMENT '配送半径（米）',
    `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入驻时间',
    `updated_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_username` (`username`),
    KEY `idx_merchant_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家/店铺（含登录凭据）';

-- ============================================================
-- 五、商品品类
-- ============================================================
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `merchant_id` BIGINT      NOT NULL                COMMENT '商家 ID',
    `name`        VARCHAR(50) NOT NULL                COMMENT '品类名称',
    `sort`        INT         DEFAULT 0               COMMENT '排序（越小越前）',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_merchant` (`merchant_id`),
    CONSTRAINT `fk_category_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品品类';

-- ============================================================
-- 六、商品
-- ============================================================
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
    KEY `idx_product_merchant` (`merchant_id`),
    KEY `idx_product_category` (`category_id`),
    CONSTRAINT `fk_product_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`),
    CONSTRAINT `fk_product_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品';

-- ============================================================
-- 七、商品规格
-- ============================================================
CREATE TABLE `product_spec` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id`  BIGINT        NOT NULL                COMMENT '商品 ID',
    `name`        VARCHAR(50)   NOT NULL                COMMENT '规格名称（如"大杯""加珍珠"）',
    `price_delta` DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '价格增量（可负）',
    `stock`       INT           NOT NULL DEFAULT 0      COMMENT '库存数量',
    PRIMARY KEY (`id`),
    KEY `idx_spec_product` (`product_id`),
    CONSTRAINT `fk_spec_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格';

-- ============================================================
-- 八、订单
-- ============================================================
CREATE TABLE `order` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_no`         VARCHAR(32)   NOT NULL                COMMENT '订单号（业务唯一）',
    `user_id`          BIGINT        NOT NULL                COMMENT '用户 ID（顾客）',
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
    KEY `idx_order_user` (`user_id`),
    KEY `idx_order_merchant` (`merchant_id`),
    KEY `idx_order_status` (`status`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_order_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单';

-- ============================================================
-- 九、订单明细
-- ============================================================
CREATE TABLE `order_item` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`       BIGINT        NOT NULL                COMMENT '订单 ID',
    `product_id`     BIGINT        NOT NULL                COMMENT '商品 ID',
    `spec_id`        BIGINT        DEFAULT NULL            COMMENT '规格 ID',
    `name_snapshot`  VARCHAR(200)  NOT NULL                COMMENT '商品+规格名称快照',
    `price_snapshot` DECIMAL(10,2) NOT NULL                COMMENT '下单时单价',
    `quantity`       INT           NOT NULL DEFAULT 1      COMMENT '购买数量',
    PRIMARY KEY (`id`),
    KEY `idx_item_order` (`order_id`),
    CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细';

-- ============================================================
-- 十、订单状态流转日志（审计）
-- ============================================================
CREATE TABLE `order_status_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT       NOT NULL                COMMENT '订单 ID',
    `from_status` VARCHAR(20)  DEFAULT NULL            COMMENT '变更前状态',
    `to_status`   VARCHAR(20)  NOT NULL                COMMENT '变更后状态',
    `operator`    VARCHAR(50)  DEFAULT NULL            COMMENT '操作人',
    `reason`      VARCHAR(500) DEFAULT NULL            COMMENT '变更原因',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_order` (`order_id`),
    CONSTRAINT `fk_log_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态流转日志';

-- ============================================================
-- 十一、支付记录
-- ============================================================
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
    UNIQUE KEY `uk_pay_order_channel` (`order_no`, `channel`),
    KEY `idx_pay_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录';

-- ============================================================
-- 十二、已上传文件
-- ============================================================
CREATE TABLE `uploaded_file` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type`    VARCHAR(30)  NOT NULL                COMMENT '业务类型：AVATAR/SHOP_LOGO/PRODUCT_IMAGE',
    `target_id`   BIGINT       DEFAULT NULL            COMMENT '关联业务对象 ID',
    `oss_key`     VARCHAR(500) NOT NULL                COMMENT 'OSS 对象 Key',
    `url`         VARCHAR(500) NOT NULL                COMMENT '文件访问 URL',
    `uploader_id` BIGINT       NOT NULL                COMMENT '上传者 ID',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_biz_type_target` (`biz_type`, `target_id`),
    KEY `idx_file_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已上传文件';

-- ============================================================
-- 十三、商家入驻申请
-- ============================================================
CREATE TABLE `merchant_application` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `applicant_user_id` BIGINT       NOT NULL                COMMENT '申请人用户 ID（顾客申请成为商家）',
    `shop_name`         VARCHAR(100) NOT NULL                COMMENT '申请店铺名称',
    `reason`            TEXT         DEFAULT NULL            COMMENT '申请理由',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
    `reviewer_id`       BIGINT       DEFAULT NULL            COMMENT '审核人 ID（管理员）',
    `reviewed_at`       DATETIME     DEFAULT NULL            COMMENT '审核时间',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (`id`),
    KEY `idx_app_applicant` (`applicant_user_id`),
    KEY `idx_app_status` (`status`),
    CONSTRAINT `fk_app_applicant` FOREIGN KEY (`applicant_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_app_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `admin` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家入驻申请';


-- ============================================================
-- ╔══════════════════════════════════════════════════════════╗
-- ║                  种 子 数 据（Seed Data）                 ║
-- ╚══════════════════════════════════════════════════════════╝
-- BCrypt hashes (cost 10):
--   admin123 → 管理员密码
--   m123456  → 商家密码
--   test123  → 测试用户密码
-- ============================================================

-- ───── 1. 平台管理员 ─────
INSERT INTO `admin` (`id`, `username`, `password`, `phone`, `status`) VALUES
(1, 'admin',     '$2b$10$LA1ZKB2DpzMatDBtHq7zSOWO9TQb.HRszyosnQkQKZRXxPILYPDSu', '13800000001', 'ACTIVE'),
(2, 'superadmin','$2b$10$LA1ZKB2DpzMatDBtHq7zSOWO9TQb.HRszyosnQkQKZRXxPILYPDSu', '13800000000', 'ACTIVE');

-- ───── 2. 普通用户（顾客） ─────
INSERT INTO `user` (`id`, `username`, `password`, `phone`, `status`) VALUES
(1, 'tester',   '$2b$10$VicOZfWSYkHPfsIw7IZscew36cAAIG21LbESuSqtdlUvJQYokaeTC', '13800000003', 'ACTIVE'),
(2, 'zhangsan', '$2b$10$VicOZfWSYkHPfsIw7IZscew36cAAIG21LbESuSqtdlUvJQYokaeTC', '13811111111', 'ACTIVE'),
(3, 'lisi',     '$2b$10$VicOZfWSYkHPfsIw7IZscew36cAAIG21LbESuSqtdlUvJQYokaeTC', '13822222222', 'ACTIVE'),
(4, 'wangwu',   '$2b$10$VicOZfWSYkHPfsIw7IZscew36cAAIG21LbESuSqtdlUvJQYokaeTC', '13833333333', 'ACTIVE'),
(5, 'zhaoliu',  '$2b$10$VicOZfWSYkHPfsIw7IZscew36cAAIG21LbESuSqtdlUvJQYokaeTC', '13844444444', 'DISABLED');

-- ───── 3. 用户收货地址 ─────
INSERT INTO `user_address` (`id`, `user_id`, `receiver`, `phone`, `detail`, `lat`, `lng`, `is_default`) VALUES
(1, 1, '测试员',   '13800000003', '北京市朝阳区望京SOHO T1-1201',   39.9963200, 116.4809000, 1),
(2, 1, '测试员公司', '13800000003', '北京市海淀区中关村软件园A座',     40.0506300, 116.2984500, 0),
(3, 2, '张三',     '13811111111', '北京市朝阳区三里屯SOHO 5号楼',    39.9321200, 116.4553000, 1),
(4, 3, '李四',     '13822222222', '北京市东城区王府井大街138号',      39.9142700, 116.4107700, 1),
(5, 4, '王五',     '13833333333', '北京市西城区金融街7号',            39.9120300, 116.3583500, 1),
(6, 2, '张三家',   '13811111111', '北京市朝阳区望京花园东区3-501',   39.9987400, 116.4741200, 0);

-- ───── 4. 商家（含登录凭据） ─────
INSERT INTO `merchant` (`id`, `username`, `password`, `name`, `phone`, `address`, `lat`, `lng`, `status`, `business_hours_start`, `business_hours_end`, `delivery_radius`) VALUES
(1, 'nightbb',  '$2b$10$0Bj3CG7mRR0.xFB7XKAGtuNhjsPeRtp1qBn7kMKV5IvBocU/2CLue', '深夜食堂',   '13800000002', '北京市朝阳区望京SOHO T1 B1-101', 39.9963200, 116.4809000, 'OPEN',    '18:00:00', '03:00:00', 5000),
(2, 'spicyhot', '$2b$10$0Bj3CG7mRR0.xFB7XKAGtuNhjsPeRtp1qBn7kMKV5IvBocU/2CLue', '辣翻天小龙虾', '13855555555', '北京市朝阳区双井桥东200米',       39.8984700, 116.4675500, 'OPEN',    '16:00:00', '02:00:00', 4000),
(3, 'teahouse', '$2b$10$0Bj3CG7mRR0.xFB7XKAGtuNhjsPeRtp1qBn7kMKV5IvBocU/2CLue', '老王糖水铺',  '13866666666', '北京市海淀区五道口地铁站B口旁',   39.9929200, 116.3383200, 'OPEN',    '14:00:00', '23:00:00', 3500);

-- ───── 5. 品类 ─────
INSERT INTO `category` (`id`, `merchant_id`, `name`, `sort`) VALUES
-- 深夜食堂
(1,  1, '烧烤类', 1),
(2,  1, '饮品类', 2),
(3,  1, '主食类', 3),
(4,  1, '凉菜类', 4),
-- 辣翻天小龙虾
(5,  2, '小龙虾', 1),
(6,  2, '烤鱼',   2),
(7,  2, '饮品类', 3),
(8,  2, '小食',   4),
-- 老王糖水铺
(9,  3, '经典糖水', 1),
(10, 3, '鲜榨果汁', 2),
(11, 3, '小吃甜品', 3);

-- ───── 6. 商品 ─────
INSERT INTO `product` (`id`, `merchant_id`, `category_id`, `name`, `price`, `status`, `description`) VALUES
-- === 深夜食堂 ===
(1,  1, 1, '羊肉串',      4.00,  'ON', '新鲜羊肉，现串现烤'),
(2,  1, 1, '牛肉串',      5.00,  'ON', '秘制腌料，外焦里嫩'),
(3,  1, 1, '烤鸡翅',      8.00,  'ON', '奥尔良风味，3只/份'),
(4,  1, 1, '烤茄子',      10.00, 'ON', '蒜蓉烤茄子，夜市王牌'),
(5,  1, 1, '烤鱿鱼',      12.00, 'ON', '整只鱿鱼，铁板现烤'),
(6,  1, 1, '烤韭菜',      5.00,  'ON', '秘制酱料，越嚼越香'),
(7,  1, 2, '冰镇酸梅汤',  6.00,  'ON', '自制酸梅汤，解腻解辣'),
(8,  1, 2, '精酿啤酒',    12.00, 'ON', '本地精酿，500ml'),
(9,  1, 2, '冰可乐',      4.00,  'ON', '330ml 罐装'),
(10, 1, 3, '炒方便面',    10.00, 'ON', '夜市经典，加蛋加肠'),
(11, 1, 3, '烤馒头',      3.00,  'ON', '外酥里软，配炼乳'),
(12, 1, 3, '蛋炒饭',      12.00, 'ON', '粒粒分明，锅气十足'),
(13, 1, 4, '拍黄瓜',      8.00,  'ON', '蒜泥陈醋，爽脆开胃'),
(14, 1, 4, '凉拌皮蛋',    10.00, 'ON', '姜汁皮蛋，经典凉菜'),

-- === 辣翻天小龙虾 ===
(15, 2, 5, '麻辣小龙虾（小份）', 68.00,  'ON', '活虾现做，麻辣鲜香（约20只）'),
(16, 2, 5, '麻辣小龙虾（大份）', 128.00, 'ON', '活虾现做，麻辣鲜香（约40只）'),
(17, 2, 5, '蒜蓉小龙虾（小份）', 68.00,  'ON', '蒜香浓郁，不吃辣首选'),
(18, 2, 5, '十三香小龙虾（大份）', 138.00, 'ON', '十三种香料慢炖入味'),
(19, 2, 6, '香辣烤鱼',    88.00,  'ON', '草鱼现烤，配时蔬（2-3人）'),
(20, 2, 6, '蒜香烤鱼',    88.00,  'ON', '草鱼现烤，蒜香扑鼻（2-3人）'),
(21, 2, 7, '冰镇酸梅汁',  8.00,   'ON', '大壶装（1.5L）'),
(22, 2, 7, '精酿扎啤',    18.00,  'ON', '1L 装'),
(23, 2, 8, '炸薯条',      12.00,  'ON', '粗薯，配番茄酱'),
(24, 2, 8, '毛豆',        10.00,  'ON', '盐水煮毛豆，下酒必备'),

-- === 老王糖水铺 ===
(25, 3, 9, '红豆沙',      10.00, 'ON', '慢火熬制，软糯香甜'),
(26, 3, 9, '绿豆沙',      8.00,  'ON', '冰镇解暑，清甜爽口'),
(27, 3, 9, '双皮奶',      12.00, 'ON', '顺德传统做法，奶香浓郁'),
(28, 3, 9, '杨枝甘露',    16.00, 'ON', '芒果+西柚+西米，港式经典'),
(29, 3, 10, '鲜榨西瓜汁', 10.00, 'ON', '当季西瓜现榨'),
(30, 3, 10, '鲜榨橙汁',   12.00, 'ON', '进口脐橙鲜榨'),
(31, 3, 11, '鸡蛋仔',     10.00, 'ON', '外脆里软，港式经典'),
(32, 3, 11, '咖喱鱼蛋',   8.00,  'ON', '港式咖喱，Q弹入味');

-- ───── 7. 商品规格 ─────
INSERT INTO `product_spec` (`id`, `product_id`, `name`, `price_delta`, `stock`) VALUES
-- 深夜食堂
(1,  1,  '小份（5串）',    0.00,  100),
(2,  1,  '大份（10串）',   3.50,  80),
(3,  2,  '小份（5串）',    0.00,  100),
(4,  2,  '大份（10串）',   4.50,  80),
(5,  3,  '3只装',          0.00,  50),
(6,  3,  '6只装',          7.00,  40),
(7,  4,  '标准份',          0.00,  60),
(8,  5,  '1只',            0.00,  40),
(9,  5,  '2只',            10.00, 30),
(10, 6,  '标准份',          0.00,  80),
(11, 7,  '杯（500ml）',    0.00,  100),
(12, 7,  '壶（1.5L）',     8.00,  40),
(13, 8,  '1瓶（500ml）',   0.00,  200),
(14, 8,  '6瓶装',          55.00, 30),
(15, 9,  '1罐',            0.00,  60),
(16, 9,  '6罐装',          0.50,  40),
(17, 10, '标配（加蛋）',    0.00,  60),
(18, 10, '豪华（加蛋加肠）', 4.00,  50),
(19, 11, '1个',            0.00,  100),
(20, 11, '3个装',          -0.50, 80),
(21, 12, '标准份',          0.00,  50),
(22, 12, '大份',           4.00,  40),
(23, 13, '标准份',          0.00,  40),
(24, 14, '标准份',          0.00,  40),

-- 辣翻天小龙虾
(25, 15, '微辣', 0.00,  30),
(26, 15, '中辣', 0.00,  30),
(27, 15, '特辣', 0.00,  20),
(28, 16, '微辣', 0.00,  20),
(29, 16, '中辣', 0.00,  20),
(30, 16, '特辣', 0.00,  15),
(31, 17, '小份', 0.00,  25),
(32, 18, '大份', 0.00,  15),
(33, 19, '微辣', 0.00,  15),
(34, 19, '中辣', 0.00,  15),
(35, 20, '不辣', 0.00,  15),
(36, 21, '1壶',  0.00,  50),
(37, 22, '1杯',  0.00,  40),
(38, 23, '标准份',0.00,  60),
(39, 24, '标准份',0.00,  60),

-- 老王糖水铺
(40, 25, '热饮', 0.00, 40),
(41, 25, '冰饮', 0.00, 40),
(42, 26, '热饮', 0.00, 40),
(43, 26, '冰饮', 0.00, 40),
(44, 27, '原味', 0.00, 30),
(45, 27, '加红豆', 2.00, 25),
(46, 28, '标准杯',0.00, 25),
(47, 29, '中杯', 0.00, 40),
(48, 29, '大杯', 4.00, 30),
(49, 30, '中杯', 0.00, 40),
(50, 30, '大杯', 4.00, 30),
(51, 31, '原味', 0.00, 35),
(52, 31, '巧克力味', 2.00, 30),
(53, 32, '小份（6粒）', 0.00, 50),
(54, 32, '大份（12粒）', 6.00, 35);

-- ───── 8. 商家入驻申请 ─────
INSERT INTO `merchant_application` (`id`, `applicant_user_id`, `shop_name`, `reason`, `status`, `reviewer_id`, `reviewed_at`) VALUES
(1, 2, '张三烧烤',   '我是张三，有3年烧烤经验，想入驻平台', 'APPROVED', 1, '2025-07-15 10:00:00'),
(2, 3, '李四炸鸡店', '自家配方，想拓展外卖业务',         'PENDING',  NULL, NULL),
(3, 4, '王五麻辣烫', '有实体店面，证照齐全',             'PENDING',  NULL, NULL),
(4, 1, '测试员深夜小厨', '测试一下入驻流程',              'REJECTED', 1, '2025-07-16 14:30:00');

-- ───── 9. 订单（用于测试） ─────
INSERT INTO `order` (`id`, `order_no`, `user_id`, `merchant_id`, `status`, `total_amount`, `address_snapshot`, `pay_channel`, `pay_time`) VALUES
(1, '20250718001', 1, 1, 'COMPLETED',  28.00, '{"receiver":"测试员","phone":"13800000003","detail":"北京市朝阳区望京SOHO T1-1201"}', 'WECHAT_MOCK', '2025-07-18 21:05:00'),
(2, '20250720001', 1, 1, 'PAID',       52.00, '{"receiver":"测试员","phone":"13800000003","detail":"北京市朝阳区望京SOHO T1-1201"}', 'ALIPAY',      '2025-07-20 19:30:00'),
(3, '20250722001', 2, 1, 'PENDING_PAY', 35.00, '{"receiver":"张三","phone":"13811111111","detail":"北京市朝阳区三里屯SOHO 5号楼"}',    NULL,          NULL),
(4, '20250722002', 3, 2, 'ACCEPTED',   156.00, '{"receiver":"李四","phone":"13822222222","detail":"北京市东城区王府井大街138号"}',       'WECHAT_MOCK', '2025-07-22 20:00:00'),
(5, '20250723001', 4, 3, 'COMPLETED',   30.00, '{"receiver":"王五","phone":"13833333333","detail":"北京市西城区金融街7号"}',             'ALIPAY',      '2025-07-23 14:00:00');

-- ───── 10. 订单明细 ─────
INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `spec_id`, `name_snapshot`, `price_snapshot`, `quantity`) VALUES
-- 订单 1（深夜食堂，28.00）
(1,  1, 1,  1,  '羊肉串 - 小份（5串）',     4.00,  2),
(2,  1, 3,  5,  '烤鸡翅 - 3只装',          8.00,  1),
(3,  1, 7,  11, '冰镇酸梅汤 - 杯（500ml）', 6.00,  2),
-- 订单 2（深夜食堂，52.00）
(4,  2, 2,  4,  '牛肉串 - 大份（10串）',    9.50,  4),
(5,  2, 7,  12, '冰镇酸梅汤 - 壶（1.5L）',  14.00, 1),
-- 订单 3（深夜食堂，35.00）
(6,  3, 2,  3,  '牛肉串 - 小份（5串）',     5.00,  3),
(7,  3, 10, 17, '炒方便面 - 标配（加蛋）',  10.00,  2),
-- 订单 4（辣翻天小龙虾，156.00）
(8,  4, 15, 25, '麻辣小龙虾（小份）- 微辣', 68.00,  2),
(9,  4, 21, 36, '冰镇酸梅汁 - 1壶',        8.00,   1),
(10, 4, 23, 38, '炸薯条 - 标准份',         12.00,  1),
-- 订单 5（老王糖水铺，30.00）
(11, 5, 26, 42, '绿豆沙 - 热饮',            8.00,   1),
(12, 5, 27, 44, '双皮奶 - 原味',            12.00,  1),
(13, 5, 31, 51, '鸡蛋仔 - 原味',            10.00,  1);

-- ───── 11. 订单状态流转日志 ─────
INSERT INTO `order_status_log` (`id`, `order_id`, `from_status`, `to_status`, `operator`, `reason`) VALUES
(1, 1, NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(2, 1, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(3, 1, 'PAID',        'ACCEPTED',    'nightbb',  '商家接单'),
(4, 1, 'ACCEPTED',    'COOKING',     'nightbb',  '开始制作'),
(5, 1, 'COOKING',     'COMPLETED',   'nightbb',  '制作完成'),
(6, 2, NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(7, 2, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(8, 3, NULL,          'PENDING_PAY', 'zhangsan', '用户下单'),
(9, 4, NULL,          'PENDING_PAY', 'lisi',     '用户下单'),
(10, 4, 'PENDING_PAY', 'PAID',       'system',   '微信支付回调'),
(11, 4, 'PAID',        'ACCEPTED',   'spicyhot', '商家接单'),
(12, 5, NULL,          'PENDING_PAY','wangwu',   '用户下单'),
(13, 5, 'PENDING_PAY', 'PAID',       'system',   '支付宝支付回调'),
(14, 5, 'PAID',        'ACCEPTED',   'teahouse', '商家接单'),
(15, 5, 'ACCEPTED',    'COOKING',    'teahouse', '开始制作'),
(16, 5, 'COOKING',     'COMPLETED',  'teahouse', '制作完成');

-- ───── 12. 支付记录 ─────
INSERT INTO `payment_record` (`id`, `order_no`, `channel`, `trade_no`, `amount`, `status`) VALUES
(1, '20250718001', 'WECHAT_MOCK', 'WM20250718001', 28.00,  'SUCCESS'),
(2, '20250720001', 'ALIPAY',      'AL20250720001', 52.00,  'SUCCESS'),
(3, '20250722002', 'WECHAT_MOCK', 'WM20250722002', 156.00, 'SUCCESS'),
(4, '20250723001', 'ALIPAY',      'AL20250723001', 30.00,  'SUCCESS');
