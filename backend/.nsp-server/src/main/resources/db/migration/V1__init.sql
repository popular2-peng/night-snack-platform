-- ============================================================
-- V1__init.sql - 夜宵外卖平台 数据库初始化（含种子数据）
-- 合并 V1 + V2 字段，扩充测试数据
-- ============================================================

-- ============================================================
-- 一、平台管理员表
-- ============================================================
CREATE TABLE `admin` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `password`   VARCHAR(200) NOT NULL                COMMENT '密码（BCrypt）',
    `phone`      VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `avatar_url` VARCHAR(500) DEFAULT NULL            COMMENT '头像 OSS URL',
    `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='平台管理员';

-- ============================================================
-- 二、普通用户表（顾客）
-- ============================================================
CREATE TABLE `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(50)  NOT NULL                COMMENT '用户名',
    `password`   VARCHAR(200) NOT NULL                COMMENT '密码（BCrypt）',
    `phone`      VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
    `avatar_url` VARCHAR(500) DEFAULT NULL            COMMENT '头像 OSS URL',
    `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    KEY `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='普通用户（顾客）';

-- ============================================================
-- 三、用户收货地址
-- ============================================================
CREATE TABLE `user_address` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT        NOT NULL,
    `receiver`   VARCHAR(50)   NOT NULL,
    `phone`      VARCHAR(20)   NOT NULL,
    `detail`     VARCHAR(500)  NOT NULL,
    `lat`        DECIMAL(10,7) NOT NULL,
    `lng`        DECIMAL(10,7) NOT NULL,
    `is_default` TINYINT(1)    NOT NULL DEFAULT 0,
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址';

-- ============================================================
-- 四、商家/店铺（V2 含全部字段）
-- ============================================================
CREATE TABLE `merchant` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
    `username`             VARCHAR(50)   NOT NULL                COMMENT '登录用户名',
    `password`             VARCHAR(200)  NOT NULL                COMMENT 'BCrypt 密码',
    `name`                 VARCHAR(100)  NOT NULL                COMMENT '店铺名称',
    `phone`                VARCHAR(20)   DEFAULT NULL            COMMENT '联系电话',
    `address`              VARCHAR(500)  DEFAULT NULL            COMMENT '店铺地址',
    `lat`                  DECIMAL(10,7) DEFAULT NULL            COMMENT '纬度',
    `lng`                  DECIMAL(10,7) DEFAULT NULL            COMMENT '经度',
    `avatar_url`           VARCHAR(500)  DEFAULT NULL            COMMENT '店铺头像 OSS URL',
    `logo_url`             VARCHAR(500)  DEFAULT NULL            COMMENT '店铺 LOGO OSS URL',
    `status`               VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/OPEN/CLOSED/SUSPENDED',
    `category_id`          BIGINT        DEFAULT NULL            COMMENT '主营品类 ID',
    `description`          TEXT          DEFAULT NULL            COMMENT '店铺简介',
    `id_card_no`           VARCHAR(50)   DEFAULT NULL            COMMENT '店主身份证号',
    `avg_rating`           DECIMAL(3,1)  DEFAULT 0.0            COMMENT '平均评分',
    `total_orders`         INT           DEFAULT 0              COMMENT '历史总订单数',
    `today_orders`         INT           DEFAULT 0              COMMENT '今日订单数(缓存)',
    `today_revenue`        DECIMAL(10,2) DEFAULT 0.00           COMMENT '今日营收(缓存)',
    `banned_reason`        VARCHAR(500)  DEFAULT NULL            COMMENT '封禁原因',
    `banned_at`            DATETIME      DEFAULT NULL            COMMENT '封禁时间',
    `banned_by`            BIGINT        DEFAULT NULL            COMMENT '封禁操作人',
    `last_login_at`        DATETIME      DEFAULT NULL            COMMENT '最后登录时间',
    `notice`               TEXT          DEFAULT NULL            COMMENT '店铺公告',
    `business_hours_start` TIME          DEFAULT NULL            COMMENT '营业开始时间',
    `business_hours_end`   TIME          DEFAULT NULL            COMMENT '营业结束时间',
    `delivery_radius`      INT           DEFAULT 5000            COMMENT '配送半径（米）',
    `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_username` (`username`),
    KEY `idx_merchant_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家/店铺';

-- ============================================================
-- 五、商品品类
-- ============================================================
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `merchant_id` BIGINT      NOT NULL,
    `name`        VARCHAR(50) NOT NULL,
    `sort`        INT         DEFAULT 0,
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category_merchant` (`merchant_id`),
    CONSTRAINT `fk_category_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品品类';

-- ============================================================
-- 六、商品
-- ============================================================
CREATE TABLE `product` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `merchant_id` BIGINT        NOT NULL,
    `category_id` BIGINT        NOT NULL,
    `name`        VARCHAR(100)  NOT NULL,
    `price`       DECIMAL(10,2) NOT NULL,
    `image_url`   VARCHAR(500)  DEFAULT NULL            COMMENT '商品图片 OSS URL',
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'ON'   COMMENT 'ON/OFF',
    `description` TEXT          DEFAULT NULL,
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
    `id`          BIGINT        NOT NULL AUTO_INCREMENT,
    `product_id`  BIGINT        NOT NULL,
    `name`        VARCHAR(50)   NOT NULL,
    `price_delta` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `stock`       INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_spec_product` (`product_id`),
    CONSTRAINT `fk_spec_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格';

-- ============================================================
-- 八、订单
-- ============================================================
CREATE TABLE `order` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`         VARCHAR(32)   NOT NULL,
    `user_id`          BIGINT        NOT NULL,
    `merchant_id`      BIGINT        NOT NULL,
    `status`           VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAY',
    `total_amount`     DECIMAL(10,2) NOT NULL,
    `address_snapshot` TEXT          DEFAULT NULL            COMMENT '收货地址快照 JSON',
    `pay_channel`      VARCHAR(20)   DEFAULT NULL            COMMENT 'ALIPAY/WECHAT_MOCK',
    `pay_time`         DATETIME      DEFAULT NULL,
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `order_id`       BIGINT        NOT NULL,
    `product_id`     BIGINT        NOT NULL,
    `spec_id`        BIGINT        DEFAULT NULL,
    `name_snapshot`  VARCHAR(200)  NOT NULL,
    `price_snapshot` DECIMAL(10,2) NOT NULL,
    `quantity`       INT           NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idx_item_order` (`order_id`),
    CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细';

-- ============================================================
-- 十、订单状态流转日志
-- ============================================================
CREATE TABLE `order_status_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`    BIGINT       NOT NULL,
    `from_status` VARCHAR(20)  DEFAULT NULL,
    `to_status`   VARCHAR(20)  NOT NULL,
    `operator`    VARCHAR(50)  DEFAULT NULL,
    `reason`      VARCHAR(500) DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_log_order` (`order_id`),
    CONSTRAINT `fk_log_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态流转日志';

-- ============================================================
-- 十一、支付记录
-- ============================================================
CREATE TABLE `payment_record` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `order_no`        VARCHAR(32)   NOT NULL,
    `channel`         VARCHAR(20)   NOT NULL,
    `trade_no`        VARCHAR(100)  DEFAULT NULL,
    `amount`          DECIMAL(10,2) NOT NULL,
    `status`          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    `notify_count`    INT           DEFAULT 0,
    `last_notify_at`  DATETIME      DEFAULT NULL,
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pay_order_channel` (`order_no`, `channel`),
    KEY `idx_pay_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录';

-- ============================================================
-- 十二、已上传文件
-- ============================================================
CREATE TABLE `uploaded_file` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `biz_type`    VARCHAR(30)  NOT NULL                COMMENT 'AVATAR/SHOP_LOGO/PRODUCT_IMAGE',
    `target_id`   BIGINT       DEFAULT NULL,
    `oss_key`     VARCHAR(500) NOT NULL,
    `url`         VARCHAR(500) NOT NULL,
    `uploader_id` BIGINT       NOT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_file_biz_type_target` (`biz_type`, `target_id`),
    KEY `idx_file_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已上传文件';

-- ============================================================
-- 十三、商家入驻申请（V2 含全部字段）
-- ============================================================
CREATE TABLE `merchant_application` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `applicant_user_id` BIGINT       NOT NULL                COMMENT '申请人用户 ID',
    `shop_name`         VARCHAR(100) NOT NULL                COMMENT '申请店铺名称',
    `reason`            TEXT         DEFAULT NULL            COMMENT '申请理由',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `reviewer_id`       BIGINT       DEFAULT NULL            COMMENT '审核人 ID',
    `reviewed_at`       DATETIME     DEFAULT NULL            COMMENT '审核时间',
    `logo_url`          VARCHAR(500) DEFAULT NULL            COMMENT '店铺 LOGO',
    `category_id`       BIGINT       DEFAULT NULL            COMMENT '主营品类 ID',
    `description`       TEXT         DEFAULT NULL            COMMENT '店铺简介',
    `id_card_no`        VARCHAR(50)  DEFAULT NULL            COMMENT '店主身份证号',
    `review_note`       TEXT         DEFAULT NULL            COMMENT '审核备注',
    `reject_reason`     VARCHAR(500) DEFAULT NULL            COMMENT '拒绝原因',
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    PRIMARY KEY (`id`),
    KEY `idx_app_applicant` (`applicant_user_id`),
    KEY `idx_app_status` (`status`),
    CONSTRAINT `fk_app_applicant` FOREIGN KEY (`applicant_user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_app_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `admin` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家入驻申请';

-- ============================================================
-- ╔══════════════════════════════════════════════════════════╗
-- ║               种 子 数 据（Seed Data）                  ║
-- ╚══════════════════════════════════════════════════════════╝
-- BCrypt (cost 10): admin→admin123, merchant→m123456, user→test123
-- 商家 LOGO 来源: picsum.photos 根据种子生成真实食物照片
-- ============================================================

-- ───── 1. 平台管理员 ─────
INSERT INTO `admin` (`id`, `username`, `password`, `phone`, `status`) VALUES
(1, 'admin',      '$2b$10$RnfmrSWD7e41HQGEWwXhXeCw982sMKxoPe2wkICq9mp2GrfNy/dpO', '13800000001', 'ACTIVE'),
(2, 'superadmin', '$2b$10$RnfmrSWD7e41HQGEWwXhXeCw982sMKxoPe2wkICq9mp2GrfNy/dpO', '13800000000', 'ACTIVE');

-- ───── 2. 普通用户（顾客）─ 13 人 ─────
INSERT INTO `user` (`id`, `username`, `password`, `phone`, `status`) VALUES
(1,  'tester',    '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13800000003', 'ACTIVE'),
(2,  'zhangsan',  '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13811111111', 'ACTIVE'),
(3,  'lisi',      '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13822222222', 'ACTIVE'),
(4,  'wangwu',    '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13833333333', 'ACTIVE'),
(5,  'zhaoliu',   '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13844444444', 'DISABLED'),
(6,  'sunqi',     '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13855551111', 'ACTIVE'),
(7,  'zhouba',    '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13866661112', 'ACTIVE'),
(8,  'wujiu',     '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13877771113', 'ACTIVE'),
(9,  'zhengshi',  '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13888881114', 'ACTIVE'),
(10, 'qianyi',    '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13899991115', 'ACTIVE'),
(11, 'liuer',     '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13800001116', 'ACTIVE'),
(12, 'caosan',    '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13811111117', 'ACTIVE'),
(13, 'weisi',     '$2b$10$o96xzY8T/91SthduiCTmUuTj/VCtyciPwlYh7z3zR7p9z82pvPPsC', '13822221118', 'DISABLED');

-- ───── 3. 用户收货地址 ─────
INSERT INTO `user_address` (`id`, `user_id`, `receiver`, `phone`, `detail`, `lat`, `lng`, `is_default`) VALUES
(1,  1,  '测试员',     '13800000003', '朝阳区望京SOHO T1-1201',         39.9963200, 116.4809000, 1),
(2,  1,  '测试员公司', '13800000003', '海淀区中关村软件园A座',           40.0506300, 116.2984500, 0),
(3,  2,  '张三',       '13811111111', '朝阳区三里屯SOHO 5号楼',          39.9321200, 116.4553000, 1),
(4,  3,  '李四',       '13822222222', '东城区王府井大街138号',            39.9142700, 116.4107700, 1),
(5,  4,  '王五',       '13833333333', '西城区金融街7号',                 39.9120300, 116.3583500, 1),
(6,  2,  '张三家',     '13811111111', '朝阳区望京花园东区3-501',         39.9987400, 116.4741200, 0),
(7,  6,  '孙七',       '13855551111', '海淀区五道口华清嘉园7-1202',      39.9932000, 116.3384000, 1),
(8,  7,  '周八',       '13866661112', '朝阳区国贸建外SOHO 9-301',        39.9087000, 116.4592000, 1),
(9,  8,  '吴九',       '13877771113', '丰台区方庄芳城园一区12-401',      39.8654000, 116.4221000, 1),
(10, 9,  '郑十',       '13888881114', '通州区万达广场B座2105',           39.9023000, 116.6575000, 1),
(11, 10, '钱一',       '13899991115', '大兴区亦庄荣华中路8号院3-102',    39.7965000, 116.5076000, 1);

-- ───── 4. 商家 — 8 家店（含 LOGO） ─────
INSERT INTO `merchant` (`id`, `username`, `password`, `name`, `phone`, `address`, `lat`, `lng`, `logo_url`,
  `status`, `category_id`, `description`, `avg_rating`, `total_orders`, `today_orders`, `today_revenue`,
  `business_hours_start`, `business_hours_end`, `delivery_radius`, `notice`) VALUES
(1, 'nightbb',   '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '深夜食堂',   '13800000002', '朝阳区望京SOHO T1 B1-101', 39.9963200, 116.4809000,
  'https://picsum.photos/seed/bbq-night/400/400',
  'OPEN', 1, '望京地区人气深夜食堂，主打烧烤+啤酒，现串现烤，凌晨不打烊！', 4.8, 1256, 23, 1860.00,
  '18:00:00', '03:00:00', 5000, '本周五推出新品炭烤羊排，欢迎品尝！'),

(2, 'spicyhot',  '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '辣翻天小龙虾','13855555555', '朝阳区双井桥东200米', 39.8984700, 116.4675500,
  'https://picsum.photos/seed/spicy-lobster/400/400',
  'OPEN', 5, '活虾现做！麻辣鲜香！北京小龙虾排行榜TOP3，支持外卖配送', 4.6, 892, 15, 2680.00,
  '16:00:00', '02:00:00', 4000, '新到一批盱眙大虾，个头更大更过瘾'),

(3, 'teahouse',   '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '老王糖水铺',  '13866666666', '海淀区五道口地铁站B口旁', 39.9929200, 116.3383200,
  'https://picsum.photos/seed/sweet-soup/400/400',
  'OPEN', 9, '30年老店，顺德传统糖水手艺传承，每一碗都是记忆中的味道', 4.9, 2100, 42, 896.00,
  '14:00:00', '23:00:00', 3500, '夏日特供冰镇系列已上线'),

(4, 'crispychick','$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '脆皮炸鸡（海淀总店）','13877778888', '海淀区中关村大街27号', 39.9836000, 116.3103000,
  'https://picsum.photos/seed/crispy-chicken/400/400',
  'OPEN', NULL, '韩式脆皮炸鸡专门店，秘制酱料10+种口味任选', 4.5, 567, 18, 1240.00,
  '10:00:00', '22:00:00', 4000, '新出蜂蜜芥末口味，买一送一！'),

(5, 'hotnoodle',  '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '川味牛肉面馆', '13888889999', '朝阳区大望路万达广场B1', 39.9093000, 116.4715000,
  'https://picsum.photos/seed/noodle-soup/400/400',
  'OPEN', NULL, '正宗重庆牛肉面，大块牛肉+秘制红油，麻辣过瘾', 4.3, 340, 8, 680.00,
  '08:00:00', '21:00:00', 3000, NULL),

(6, 'teamilk',    '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '元气奶茶实验室','13866667777', '海淀区学院路15号', 39.9888000, 116.3475000,
  'https://picsum.photos/seed/bubble-tea/400/400',
  'OPEN', NULL, '年轻人的奶茶实验室，每周一款限定新品，好喝又好玩', 4.7, 1890, 56, 1568.00,
  '09:00:00', '22:00:00', 5000, '本周限定：杨枝甘露波波冰'),

(7, 'pizzaking',  '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '披萨大王',    '13855556666', '东城区东直门银座B1', 39.9406000, 116.4352000,
  'https://picsum.photos/seed/pizza-king/400/400',
  'CLOSED', NULL, '18寸超大披萨，手工现做，芝士拉丝超过1米', 4.1, 210, 0, 0.00,
  '10:00:00', '21:00:00', 6000, '店铺装修中，预计8月重新开业'),

(8, 'malatang',   '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '杨国福麻辣烫（望京店）','13812345678', '朝阳区望京阜通东大街6号', 39.9985000, 116.4812000,
  'https://picsum.photos/seed/malatang/400/400',
  'SUSPENDED', NULL, '东北正宗麻辣烫，骨汤熬制，食材新鲜，价格实惠', 3.9, 450, 0, 0.00,
  '08:00:00', '22:00:00', 3000, NULL);

-- ───── 5. 品类 ─────
INSERT INTO `category` (`id`, `merchant_id`, `name`, `sort`) VALUES
-- 深夜食堂
(1,  1, '烧烤类', 1), (2,  1, '饮品类', 2), (3,  1, '主食类', 3), (4,  1, '凉菜类', 4),
-- 辣翻天小龙虾
(5,  2, '小龙虾', 1), (6,  2, '烤鱼',   2), (7,  2, '饮品类', 3), (8,  2, '小食',   4),
-- 老王糖水铺
(9,  3, '经典糖水', 1), (10, 3, '鲜榨果汁', 2), (11, 3, '小吃甜品', 3),
-- 脆皮炸鸡
(12, 4, '炸鸡套餐', 1), (13, 4, '小食配菜', 2), (14, 4, '饮品', 3),
-- 川味牛肉面馆
(15, 5, '面食类', 1), (16, 5, '小菜类', 2), (17, 5, '饮品类', 3),
-- 元气奶茶
(18, 6, '招牌奶茶', 1), (19, 6, '水果茶', 2), (20, 6, '奶盖系列', 3),
-- 披萨大王
(21, 7, '经典披萨', 1), (22, 7, '意面焗饭', 2), (23, 7, '小食饮品', 3),
-- 杨国福麻辣烫
(24, 8, '自选麻辣烫', 1), (25, 8, '麻辣拌', 2), (26, 8, '饮品', 3);

-- ───── 6. 商品 ─────
INSERT INTO `product` (`id`, `merchant_id`, `category_id`, `name`, `price`, `status`, `description`) VALUES
-- === 深夜食堂 (1) ===
(1,  1, 1, '羊肉串',      4.00,  'ON', '新鲜羊肉，现串现烤'),
(2,  1, 1, '牛肉串',      5.00,  'ON', '秘制腌料，外焦里嫩'),
(3,  1, 1, '烤鸡翅',      8.00,  'ON', '奥尔良风味，3只/份'),
(4,  1, 1, '烤茄子',      10.00, 'ON', '蒜蓉烤茄子，夜市王牌'),
(5,  1, 1, '烤鱿鱼',      12.00, 'ON', '整只鱿鱼铁板现烤'),
(6,  1, 1, '烤韭菜',      5.00,  'ON', '秘制酱料越嚼越香'),
(7,  1, 2, '冰镇酸梅汤',  6.00,  'ON', '自制酸梅汤解腻解辣'),
(8,  1, 2, '精酿啤酒',    12.00, 'ON', '本地精酿500ml'),
(9,  1, 2, '冰可乐',      4.00,  'ON', '330ml罐装'),
(10, 1, 3, '炒方便面',    10.00, 'ON', '夜市经典加蛋加肠'),
(11, 1, 3, '烤馒头',      3.00,  'ON', '外酥里软配炼乳'),
(12, 1, 3, '蛋炒饭',      12.00, 'ON', '粒粒分明锅气十足'),
(13, 1, 4, '拍黄瓜',      8.00,  'ON', '蒜泥陈醋爽脆开胃'),
(14, 1, 4, '凉拌皮蛋',    10.00, 'ON', '姜汁皮蛋经典凉菜'),

-- === 辣翻天小龙虾 (2) ===
(15, 2, 5, '麻辣小龙虾（小份）', 68.00,  'ON', '活虾现做麻辣鲜香(约20只)'),
(16, 2, 5, '麻辣小龙虾（大份）', 128.00, 'ON', '活虾现做麻辣鲜香(约40只)'),
(17, 2, 5, '蒜蓉小龙虾（小份）', 68.00,  'ON', '蒜香浓郁不吃辣首选'),
(18, 2, 5, '十三香小龙虾（大份）',138.00,'ON', '十三种香料慢炖入味'),
(19, 2, 6, '香辣烤鱼',    88.00,  'ON', '草鱼现烤配时蔬(2-3人)'),
(20, 2, 6, '蒜香烤鱼',    88.00,  'ON', '草鱼现烤蒜香扑鼻(2-3人)'),
(21, 2, 7, '冰镇酸梅汁',  8.00,   'ON', '大壶装1.5L'),
(22, 2, 7, '精酿扎啤',    18.00,  'ON', '1L装'),
(23, 2, 8, '炸薯条',      12.00,  'ON', '粗薯配番茄酱'),
(24, 2, 8, '盐水毛豆',    10.00,  'ON', '下酒必备'),

-- === 老王糖水铺 (3) ===
(25, 3, 9,  '红豆沙',     10.00, 'ON', '慢火熬制软糯香甜'),
(26, 3, 9,  '绿豆沙',     8.00,  'ON', '冰镇解暑清甜爽口'),
(27, 3, 9,  '双皮奶',     12.00, 'ON', '顺德传统做法奶香浓郁'),
(28, 3, 9,  '杨枝甘露',   16.00, 'ON', '芒果+西柚+西米港式经典'),
(29, 3, 10, '鲜榨西瓜汁', 10.00, 'ON', '当季西瓜现榨'),
(30, 3, 10, '鲜榨橙汁',   12.00, 'ON', '进口脐橙鲜榨'),
(31, 3, 11, '鸡蛋仔',     10.00, 'ON', '外脆里软港式经典'),
(32, 3, 11, '咖喱鱼蛋',   8.00,  'ON', '港式咖喱Q弹入味'),

-- === 脆皮炸鸡 (4) ===
(33, 4, 12, '原味脆皮炸鸡（半只）', 28.00, 'ON', '外酥里嫩汁水丰富'),
(34, 4, 12, '原味脆皮炸鸡（整只）', 48.00, 'ON', '整鸡现炸适合2-3人'),
(35, 4, 12, '韩式甜辣炸鸡', 32.00, 'ON', '甜辣酱裹满每一块鸡肉'),
(36, 4, 13, '炸薯角',     10.00, 'ON', '大块土豆现炸外酥里糯'),
(37, 4, 13, '洋葱圈',     8.00,  'ON', '金黄酥脆'),
(38, 4, 14, '冰可乐',     5.00,  'ON', '500ml'),
(39, 4, 14, '柠檬水',     6.00,  'ON', '鲜柠檬现做'),

-- === 川味牛肉面馆 (5) ===
(40, 5, 15, '招牌牛肉面', 22.00, 'ON', '大块牛腱子+手工面条'),
(41, 5, 15, '红烧肥肠面', 20.00, 'ON', '肥肠软糯入味'),
(42, 5, 15, '担担面',     16.00, 'ON', '正宗四川担担面'),
(43, 5, 16, '红油抄手',   14.00, 'ON', '皮薄馅大配红油'),
(44, 5, 16, '口水鸡',     18.00, 'ON', '麻辣鲜香'),
(45, 5, 17, '冰粉',       8.00,  'ON', '手搓冰粉红糖底'),

-- === 元气奶茶实验室 (6) ===
(46, 6, 18, '经典珍珠奶茶', 14.00, 'ON', '黑糖珍珠+鲜奶'),
(47, 6, 18, '波霸奶茶',   16.00, 'ON', '超大颗波霸'),
(48, 6, 19, '满杯鲜橙',   18.00, 'ON', '整颗鲜橙现榨'),
(49, 6, 19, '多肉葡萄',   22.00, 'ON', '手剥葡萄+芝士奶盖'),
(50, 6, 20, '芝士奶盖绿茶',16.00, 'ON', '咸香芝士奶盖'),
(51, 6, 20, '黑糖脏脏茶', 18.00, 'ON', '黑糖挂壁+鲜奶'),

-- === 披萨大王 (7) ===
(52, 7, 21, '超级至尊披萨(12寸)', 68.00, 'ON', '6种肉类+4种蔬菜'),
(53, 7, 21, '榴莲披萨(12寸)',     78.00, 'ON', '金枕榴莲果肉满满'),
(54, 7, 22, '番茄肉酱意面',       28.00, 'ON', '手工番茄酱+牛肉末'),
(55, 7, 22, '芝士焗饭',           24.00, 'ON', '马苏里拉芝士焗饭'),
(56, 7, 23, '鸡米花',             12.00, 'ON', '配甜辣酱'),

-- === 杨国福麻辣烫 (8) ===
(57, 8, 24, '自选麻辣烫（小份）', 16.00, 'ON', '任选6种菜+2种肉'),
(58, 8, 24, '自选麻辣烫（大份）', 26.00, 'ON', '任选10种菜+4种肉'),
(59, 8, 25, '麻辣拌',             18.00, 'ON', '东北特色干拌麻辣烫'),
(60, 8, 26, '酸梅汤',             5.00,  'ON', '自制冰镇酸梅汤');

-- ───── 7. 商品规格 ─────
INSERT INTO `product_spec` (`id`, `product_id`, `name`, `price_delta`, `stock`) VALUES
-- 深夜食堂
(1,  1,  '小份(5串)',  0.00,  100), (2,  1,  '大份(10串)', 3.50,  80),
(3,  2,  '小份(5串)',  0.00,  100), (4,  2,  '大份(10串)', 4.50,  80),
(5,  3,  '3只装',      0.00,  50),  (6,  3,  '6只装',     7.00,  40),
(7,  4,  '标准份',     0.00,  60),  (8,  5,  '1只',       0.00,  40),
(9,  5,  '2只',        10.00, 30),  (10, 6,  '标准份',     0.00,  80),
(11, 7,  '杯(500ml)',  0.00,  100), (12, 7,  '壶(1.5L)',  8.00,  40),
(13, 8,  '1瓶(500ml)', 0.00,  200), (14, 8,  '6瓶装',     55.00, 30),
(15, 9,  '1罐',        0.00,  60),  (16, 9,  '6罐装',     0.50,  40),
(17, 10, '标配(加蛋)',  0.00,  60),  (18, 10, '豪华(加蛋加肠)',4.00,50),
(19, 11, '1个',        0.00,  100), (20, 11, '3个装',     -0.50, 80),
(21, 12, '标准份',     0.00,  50),  (22, 12, '大份',      4.00,  40),
(23, 13, '标准份',     0.00,  40),  (24, 14, '标准份',     0.00,  40),
-- 辣翻天
(25, 15, '微辣', 0.00, 30), (26, 15, '中辣', 0.00, 30), (27, 15, '特辣', 0.00, 20),
(28, 16, '微辣', 0.00, 20), (29, 16, '中辣', 0.00, 20), (30, 16, '特辣', 0.00, 15),
(31, 17, '小份', 0.00, 25), (32, 18, '大份', 0.00, 15),
(33, 19, '微辣', 0.00, 15), (34, 19, '中辣', 0.00, 15),
(35, 20, '不辣', 0.00, 15),
(36, 21, '1壶', 0.00, 50), (37, 22, '1杯', 0.00, 40),
(38, 23, '标准份', 0.00, 60), (39, 24, '标准份', 0.00, 60),
-- 老王糖水铺
(40, 25, '热饮', 0.00, 40), (41, 25, '冰饮', 0.00, 40),
(42, 26, '热饮', 0.00, 40), (43, 26, '冰饮', 0.00, 40),
(44, 27, '原味', 0.00, 30), (45, 27, '加红豆', 2.00, 25),
(46, 28, '标准杯', 0.00, 25),
(47, 29, '中杯', 0.00, 40), (48, 29, '大杯', 4.00, 30),
(49, 30, '中杯', 0.00, 40), (50, 30, '大杯', 4.00, 30),
(51, 31, '原味', 0.00, 35), (52, 31, '巧克力味', 2.00, 30),
(53, 32, '小份(6粒)', 0.00, 50), (54, 32, '大份(12粒)', 6.00, 35),
-- 脆皮炸鸡
(55, 33, '半只', 0.00, 50), (56, 34, '整只', 0.00, 30),
(57, 35, '标准份', 0.00, 40),
(58, 36, '标准份', 0.00, 60), (59, 37, '标准份', 0.00, 50),
(60, 38, '1杯', 0.00, 80),  (61, 39, '1杯', 0.00, 60),
-- 川味牛肉面馆
(62, 40, '标准份', 0.00, 50), (63, 40, '加面加肉', 8.00, 30),
(64, 41, '标准份', 0.00, 40), (65, 42, '标准份', 0.00, 40),
(66, 43, '标准份', 0.00, 50), (67, 44, '标准份', 0.00, 30),
(68, 45, '标准份', 0.00, 60),
-- 元气奶茶
(69, 46, '中杯', 0.00, 80), (70, 46, '大杯', 4.00, 60),
(71, 47, '中杯', 0.00, 60), (72, 48, '标准杯', 0.00, 50),
(73, 49, '标准杯', 0.00, 40), (74, 50, '标准杯', 0.00, 50),
(75, 51, '标准杯', 0.00, 50),
-- 披萨大王
(76, 52, '12寸', 0.00, 30), (77, 53, '12寸', 0.00, 20),
(78, 54, '标准份', 0.00, 40), (79, 55, '标准份', 0.00, 40),
(80, 56, '标准份', 0.00, 60),
-- 杨国福麻辣烫
(81, 57, '微辣', 0.00, 60), (82, 57, '中辣', 0.00, 60), (83, 57, '麻辣', 0.00, 50),
(84, 58, '微辣', 0.00, 40), (85, 58, '中辣', 0.00, 40), (86, 58, '麻辣', 0.00, 30),
(87, 59, '标准份', 0.00, 50), (88, 60, '1杯', 0.00, 80);

-- ───── 8. 商家入驻申请 ─────
INSERT INTO `merchant_application` (`id`, `applicant_user_id`, `shop_name`, `reason`, `status`, `reviewer_id`, `reviewed_at`, `logo_url`, `description`, `category_id`) VALUES
(1, 2,  '张三烧烤',   '3年烧烤经验，有实体店，想入驻线上平台', 'APPROVED', 1, '2025-07-15 10:00:00',
  'https://picsum.photos/seed/zhangsan-bbq/400/400', '东北烧烤老店，秘制配方', 1),
(2, 3,  '李四炸鸡店', '自家配方+实体店面，想拓展外卖业务', 'PENDING', NULL, NULL,
  'https://picsum.photos/seed/lisi-chicken/400/400', '韩式炸鸡专门店，10+口味', 12),
(3, 4,  '王五麻辣烫', '证照齐全，有3年餐饮经验', 'PENDING', NULL, NULL,
  'https://picsum.photos/seed/wangwu-malatang/400/400', '正宗川味麻辣烫，骨汤熬制', 24),
(4, 1,  '测试深夜小厨','测试入驻流程', 'REJECTED', 1, '2025-07-16 14:30:00',
  NULL, '测试数据', NULL),
(5, 8,  '吴九日料居酒屋', '在日本学厨5年，想在望京开一家居酒屋', 'PENDING', NULL, NULL,
  'https://picsum.photos/seed/japanese-izakaya/400/400', '东京风味居酒屋，刺身+烤物+清酒', NULL),
(6, 9,  '郑十螺蛳粉', '柳州学艺归来，正宗螺蛳粉', 'PENDING', NULL, NULL,
  'https://picsum.photos/seed/luosifen/400/400', '柳州正宗螺蛳粉，酸笋够味', NULL);

-- ───── 9. 订单 — 20 条（覆盖各种状态、跨商家） ─────
INSERT INTO `order` (`id`, `order_no`, `user_id`, `merchant_id`, `status`, `total_amount`, `address_snapshot`, `pay_channel`, `pay_time`) VALUES
(1,  '20250718001', 1,  1, 'COMPLETED',   28.00,  '{"receiver":"测试员","phone":"13800000003","detail":"朝阳区望京SOHO T1-1201"}', 'WECHAT_MOCK', '2025-07-18 21:05:00'),
(2,  '20250720001', 1,  1, 'PAID',        52.00,  '{"receiver":"测试员","phone":"13800000003","detail":"朝阳区望京SOHO T1-1201"}', 'ALIPAY',      '2025-07-20 19:30:00'),
(3,  '20250722001', 2,  1, 'PENDING_PAY',  35.00,  '{"receiver":"张三","phone":"13811111111","detail":"朝阳区三里屯SOHO 5号楼"}', NULL, NULL),
(4,  '20250722002', 3,  2, 'ACCEPTED',    156.00,  '{"receiver":"李四","phone":"13822222222","detail":"东城区王府井大街138号"}', 'WECHAT_MOCK', '2025-07-22 20:00:00'),
(5,  '20250723001', 4,  3, 'COMPLETED',    30.00,  '{"receiver":"王五","phone":"13833333333","detail":"西城区金融街7号"}', 'ALIPAY',      '2025-07-23 14:00:00'),
(6,  '20250724001', 6,  2, 'COOKING',     136.00,  '{"receiver":"孙七","phone":"13855551111","detail":"海淀区五道口华清嘉园7-1202"}', 'WECHAT_MOCK', '2025-07-24 18:30:00'),
(7,  '20250724002', 2,  3, 'COMPLETED',    26.00,  '{"receiver":"张三","phone":"13811111111","detail":"朝阳区三里屯SOHO 5号楼"}', 'WECHAT_MOCK', '2025-07-24 15:00:00'),
(8,  '20250724003', 7,  1, 'PAID',         48.00,  '{"receiver":"周八","phone":"13866661112","detail":"朝阳区国贸建外SOHO 9-301"}', 'ALIPAY',      '2025-07-24 20:15:00'),
(9,  '20250724004', 8,  4, 'ACCEPTED',     32.00,  '{"receiver":"吴九","phone":"13877771113","detail":"丰台区方庄芳城园一区12-401"}', 'ALIPAY', '2025-07-24 19:00:00'),
(10, '20250724005', 10, 6, 'COMPLETED',    36.00,  '{"receiver":"钱一","phone":"13899991115","detail":"大兴区亦庄荣华中路8号院3-102"}', 'WECHAT_MOCK', '2025-07-24 14:00:00'),
(11, '20250724006', 9,  5, 'PENDING_PAY',  22.00,  '{"receiver":"郑十","phone":"13888881114","detail":"通州区万达广场B座2105"}', NULL, NULL),
(12, '20250725001', 1,  6, 'ACCEPTED',     52.00,  '{"receiver":"测试员","phone":"13800000003","detail":"朝阳区望京SOHO T1-1201"}', 'WECHAT_MOCK', '2025-07-25 12:30:00'),
(13, '20250725002', 3,  4, 'COMPLETED',    28.00,  '{"receiver":"李四","phone":"13822222222","detail":"东城区王府井大街138号"}', 'ALIPAY', '2025-07-25 11:00:00'),
(14, '20250725003', 6,  3, 'COOKING',      40.00,  '{"receiver":"孙七","phone":"13855551111","detail":"海淀区五道口华清嘉园7-1202"}', 'WECHAT_MOCK', '2025-07-25 16:00:00'),
(15, '20250725004', 2,  5, 'CANCELLED',    20.00,  '{"receiver":"张三","phone":"13811111111","detail":"朝阳区三里屯SOHO 5号楼"}', NULL, NULL),
(16, '20250725005', 11, 6, 'PENDING_PAY',  18.00,  '{"receiver":"刘二","phone":"13800001116","detail":"海淀区知春路56号"}', NULL, NULL),
(17, '20250725006', 12, 7, 'COMPLETED',    68.00,  '{"receiver":"曹三","phone":"13811111117","detail":"东城区鼓楼东大街12号"}', 'ALIPAY', '2025-07-25 13:00:00'),
(18, '20250725007', 7,  2, 'REFUNDING',   156.00,  '{"receiver":"周八","phone":"13866661112","detail":"朝阳区国贸建外SOHO 9-301"}', 'ALIPAY', '2025-07-25 10:00:00'),
(19, '20250725008', 4,  1, 'PAID',         16.00,  '{"receiver":"王五","phone":"13833333333","detail":"西城区金融街7号"}', 'WECHAT_MOCK', '2025-07-25 20:00:00'),
(20, '20250725009', 8,  3, 'ACCEPTED',     22.00,  '{"receiver":"吴九","phone":"13877771113","detail":"丰台区方庄芳城园一区12-401"}', 'WECHAT_MOCK', '2025-07-25 18:00:00');

-- ───── 10. 订单明细 ─────
INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `spec_id`, `name_snapshot`, `price_snapshot`, `quantity`) VALUES
-- 订单1: 深夜食堂 COMPLETED 28.00
(1,  1, 1,  1,  '羊肉串-小份(5串)',     4.00,  2),
(2,  1, 3,  5,  '烤鸡翅-3只装',          8.00,  1),
(3,  1, 7,  11, '冰镇酸梅汤-杯(500ml)',  6.00,  2),
-- 订单2: 深夜食堂 PAID 52.00
(4,  2, 2,  4,  '牛肉串-大份(10串)',     9.50,  4),
(5,  2, 7,  12, '冰镇酸梅汤-壶(1.5L)',   14.00, 1),
-- 订单3: 深夜食堂 PENDING_PAY 35.00
(6,  3, 2,  3,  '牛肉串-小份(5串)',      5.00,  3),
(7,  3, 10, 17, '炒方便面-标配(加蛋)',   10.00,  2),
-- 订单4: 辣翻天 ACCEPTED 156.00
(8,  4, 15, 25, '麻辣小龙虾(小份)-微辣', 68.00,  2),
(9,  4, 21, 36, '冰镇酸梅汁-1壶',        8.00,   1),
(10, 4, 23, 38, '炸薯条-标准份',         12.00,  1),
-- 订单5: 老王糖水铺 COMPLETED 30.00
(11, 5, 26, 42, '绿豆沙-热饮',           8.00,   1),
(12, 5, 27, 44, '双皮奶-原味',           12.00,  1),
(13, 5, 31, 51, '鸡蛋仔-原味',           10.00,  1),
-- 订单6: 辣翻天 COOKING 136.00
(14, 6, 16, 28, '麻辣小龙虾(大份)-微辣', 128.00, 1),
(15, 6, 21, 36, '冰镇酸梅汁-1壶',        8.00,   1),
-- 订单7: 老王糖水铺 COMPLETED 26.00
(16, 7, 28, 46, '杨枝甘露-标准杯',       16.00,  1),
(17, 7, 32, 53, '咖喱鱼蛋-小份(6粒)',    10.00,  1),
-- 订单8: 深夜食堂 PAID 48.00
(18, 8, 4,  7,  '烤茄子-标准份',         10.00,  1),
(19, 8, 5,  8,  '烤鱿鱼-1只',           12.00,  1),
(20, 8, 2,  4,  '牛肉串-大份(10串)',     9.50,  2),
(21, 8, 9,  15, '冰可乐-1罐',            4.00,  1),
-- 订单9: 脆皮炸鸡 ACCEPTED 32.00
(22, 9, 35, 57, '韩式甜辣炸鸡-标准份',  32.00,  1),
-- 订单10: 元气奶茶 COMPLETED 36.00
(23, 10, 49, 73, '多肉葡萄-标准杯',      22.00,  1),
(24, 10, 46, 69, '经典珍珠奶茶-中杯',    14.00,  1),
-- 订单11: 川味牛肉面馆 PENDING_PAY 22.00
(25, 11, 40, 62, '招牌牛肉面-标准份',    22.00,  1),
-- 订单12: 元气奶茶 ACCEPTED 52.00
(26, 12, 49, 73, '多肉葡萄-标准杯',      22.00,  2),
(27, 12, 50, 74, '芝士奶盖绿茶-标准杯',  16.00,  1),
-- 订单13: 脆皮炸鸡 COMPLETED 28.00
(28, 13, 33, 55, '原味脆皮炸鸡-半只',   28.00,  1),
-- 订单14: 老王糖水铺 COOKING 40.00
(29, 14, 28, 46, '杨枝甘露-标准杯',      16.00,  2),
(30, 14, 31, 51, '鸡蛋仔-原味',          10.00,  1),
-- 订单15: 川味牛肉面馆 CANCELLED
(31, 15, 40, 62, '招牌牛肉面-标准份',    20.00,  1),
-- 订单16: 元气奶茶 PENDING_PAY 18.00
(32, 16, 51, 75, '黑糖脏脏茶-标准杯',    18.00,  1),
-- 订单17: 披萨大王 COMPLETED 68.00
(33, 17, 52, 76, '超级至尊披萨-12寸',    68.00,  1),
-- 订单18: 辣翻天 REFUNDING 156.00
(34, 18, 16, 28, '麻辣小龙虾(大份)-微辣',128.00, 1),
(35, 18, 24, 39, '盐水毛豆-标准份',      10.00,  1),
(36, 18, 22, 37, '精酿扎啤-1杯',        18.00,  1),
-- 订单19: 深夜食堂 PAID 16.00
(37, 19, 1,  1,  '羊肉串-小份(5串)',     4.00,  4),
-- 订单20: 老王糖水铺 ACCEPTED 22.00
(38, 20, 25, 41, '红豆沙-冰饮',          10.00,  1),
(39, 20, 30, 50, '鲜榨橙汁-大杯',        12.00,  1);

-- ───── 11. 订单状态流转日志 ─────
INSERT INTO `order_status_log` (`id`, `order_id`, `from_status`, `to_status`, `operator`, `reason`) VALUES
(1,  1,  NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(2,  1,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(3,  1,  'PAID',        'ACCEPTED',    'nightbb',  '商家接单'),
(4,  1,  'ACCEPTED',    'COOKING',     'nightbb',  '开始制作'),
(5,  1,  'COOKING',     'COMPLETED',   'nightbb',  '制作完成'),
(6,  2,  NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(7,  2,  'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(8,  3,  NULL,          'PENDING_PAY', 'zhangsan', '用户下单'),
(9,  4,  NULL,          'PENDING_PAY', 'lisi',     '用户下单'),
(10, 4,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(11, 4,  'PAID',        'ACCEPTED',    'spicyhot', '商家接单'),
(12, 5,  NULL,          'PENDING_PAY', 'wangwu',   '用户下单'),
(13, 5,  'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(14, 5,  'PAID',        'ACCEPTED',    'teahouse', '商家接单'),
(15, 5,  'ACCEPTED',    'COOKING',     'teahouse', '开始制作'),
(16, 5,  'COOKING',     'COMPLETED',   'teahouse', '制作完成'),
(17, 6,  NULL,          'PENDING_PAY', 'sunqi',    '用户下单'),
(18, 6,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(19, 6,  'PAID',        'ACCEPTED',    'spicyhot', '商家接单'),
(20, 6,  'ACCEPTED',    'COOKING',     'spicyhot', '开始制作'),
(21, 7,  NULL,          'PENDING_PAY', 'zhangsan', '用户下单'),
(22, 7,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(23, 7,  'PAID',        'ACCEPTED',    'teahouse', '商家接单'),
(24, 7,  'ACCEPTED',    'COOKING',     'teahouse', '开始制作'),
(25, 7,  'COOKING',     'COMPLETED',   'teahouse', '制作完成'),
(26, 8,  NULL,          'PENDING_PAY', 'zhouba',   '用户下单'),
(27, 8,  'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(28, 9,  NULL,          'PENDING_PAY', 'wujiu',    '用户下单'),
(29, 9,  'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(30, 9,  'PAID',        'ACCEPTED',    'crispychick','商家接单'),
(31, 10, NULL,          'PENDING_PAY', 'qianyi',   '用户下单'),
(32, 10, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(33, 10, 'PAID',        'ACCEPTED',    'teamilk',  '商家接单'),
(34, 10, 'ACCEPTED',    'COOKING',     'teamilk',  '开始制作'),
(35, 10, 'COOKING',     'COMPLETED',   'teamilk',  '制作完成'),
(36, 15, NULL,          'PENDING_PAY', 'zhangsan', '用户下单'),
(37, 15, 'PENDING_PAY', 'CANCELLED',   'zhangsan', '用户取消订单');

-- ───── 12. 支付记录 ─────
INSERT INTO `payment_record` (`id`, `order_no`, `channel`, `trade_no`, `amount`, `status`) VALUES
(1,  '20250718001', 'WECHAT_MOCK', 'WM20250718001', 28.00,  'SUCCESS'),
(2,  '20250720001', 'ALIPAY',      'AL20250720001', 52.00,  'SUCCESS'),
(3,  '20250722002', 'WECHAT_MOCK', 'WM20250722002', 156.00, 'SUCCESS'),
(4,  '20250723001', 'ALIPAY',      'AL20250723001', 30.00,  'SUCCESS'),
(5,  '20250724001', 'WECHAT_MOCK', 'WM20250724001', 136.00, 'SUCCESS'),
(6,  '20250724002', 'WECHAT_MOCK', 'WM20250724002', 26.00,  'SUCCESS'),
(7,  '20250724003', 'ALIPAY',      'AL20250724003', 48.00,  'SUCCESS'),
(8,  '20250724004', 'ALIPAY',      'AL20250724004', 32.00,  'SUCCESS'),
(9,  '20250724005', 'WECHAT_MOCK', 'WM20250724005', 36.00,  'SUCCESS'),
(10, '20250725001', 'WECHAT_MOCK', 'WM20250725001', 52.00,  'SUCCESS'),
(11, '20250725002', 'ALIPAY',      'AL20250725002', 28.00,  'SUCCESS'),
(12, '20250725003', 'WECHAT_MOCK', 'WM20250725003', 40.00,  'SUCCESS'),
(13, '20250725005', 'ALIPAY',      'AL20250725005', 68.00,  'SUCCESS'),
(14, '20250725007', 'ALIPAY',      'AL20250725007', 156.00, 'SUCCESS'),
(15, '20250725008', 'WECHAT_MOCK', 'WM20250725008', 16.00,  'SUCCESS'),
(16, '20250725009', 'WECHAT_MOCK', 'WM20250725009', 22.00,  'SUCCESS');
