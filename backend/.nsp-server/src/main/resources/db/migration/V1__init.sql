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
    `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
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
    `status`     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DISABLED',
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
    `receiver`   VARCHAR(50)   NOT NULL                COMMENT '收货人姓名',
    `phone`      VARCHAR(20)   NOT NULL                COMMENT '收货人电话',
    `detail`     VARCHAR(500)  NOT NULL                COMMENT '详细地址',
    `lat`        DECIMAL(10,7) NOT NULL                COMMENT '纬度',
    `lng`        DECIMAL(10,7) NOT NULL                COMMENT '经度',
    `is_default` TINYINT(1)    NOT NULL DEFAULT 0      COMMENT '是否默认地址：0-否 1-是',
    `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收货地址';

-- ============================================================
-- 四、商家/店铺（V2 含全部字段）
-- ============================================================
CREATE TABLE `merchant` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`             VARCHAR(50)   NOT NULL                COMMENT '登录用户名',
    `password`             VARCHAR(200)  NOT NULL                COMMENT '登录密码（BCrypt）',
    `name`                 VARCHAR(100)  NOT NULL                COMMENT '店铺名称',
    `phone`                VARCHAR(20)   DEFAULT NULL            COMMENT '联系电话',
    `address`              VARCHAR(500)  DEFAULT NULL            COMMENT '店铺地址',
    `lat`                  DECIMAL(10,7) DEFAULT NULL            COMMENT '纬度',
    `lng`                  DECIMAL(10,7) DEFAULT NULL            COMMENT '经度',
    `avatar_url`           VARCHAR(500)  DEFAULT NULL            COMMENT '店铺头像 OSS URL（旧字段）',
    `logo_url`             VARCHAR(500)  DEFAULT NULL            COMMENT '店铺 LOGO OSS URL',
    `status`               VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '营业状态：PENDING/OPEN/CLOSED/SUSPENDED',
    `category_id`          BIGINT        DEFAULT NULL            COMMENT '主营品类 ID',
    `description`          TEXT          DEFAULT NULL            COMMENT '店铺简介/招牌描述',
    `id_card_no`           VARCHAR(50)   DEFAULT NULL            COMMENT '店主身份证号（加密）',
    `avg_rating`           DECIMAL(3,1)  DEFAULT 0.0            COMMENT '店铺平均评分',
    `total_orders`         INT           DEFAULT 0              COMMENT '历史总订单数',
    `banned_reason`        VARCHAR(500)  DEFAULT NULL            COMMENT '封禁原因',
    `banned_at`            DATETIME      DEFAULT NULL            COMMENT '封禁时间',
    `banned_by`            BIGINT        DEFAULT NULL            COMMENT '封禁操作人 ID',
    `last_login_at`        DATETIME      DEFAULT NULL            COMMENT '最后登录时间',
    `notice`               TEXT          DEFAULT NULL            COMMENT '店铺公告/营业备注',
    `business_hours_start` TIME          DEFAULT NULL            COMMENT '营业开始时间',
    `business_hours_end`   TIME          DEFAULT NULL            COMMENT '营业结束时间',
    `delivery_radius`      INT           DEFAULT 5000            COMMENT '配送半径（米）',
    `created_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入驻时间',
    `updated_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_merchant_username` (`username`),
    KEY `idx_merchant_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商家/店铺';

-- ============================================================
-- 五、商品品类
-- ============================================================
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `merchant_id` BIGINT      NOT NULL                COMMENT '商家 ID',
    `name`        VARCHAR(50) NOT NULL                COMMENT '品类名称',
    `sort`        INT         DEFAULT 0               COMMENT '排序序号',
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
    `price`       DECIMAL(10,2) NOT NULL                COMMENT '商品价格',
    `image_url`   VARCHAR(500)  DEFAULT NULL            COMMENT '商品图片 OSS URL',
    `status`      VARCHAR(20)   NOT NULL DEFAULT 'ON'   COMMENT '状态：ON-上架 / OFF-下架',
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
    `price_delta` DECIMAL(10,2) NOT NULL DEFAULT 0.00   COMMENT '价格差额（可为负）',
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
    `user_id`          BIGINT        NOT NULL                COMMENT '下单用户 ID',
    `merchant_id`      BIGINT        NOT NULL                COMMENT '商家 ID',
    `status`           VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态：PENDING_PAY/PAID/ACCEPTED/COOKING/COMPLETED/CANCELLED/REFUNDING/REFUNDED',
    `total_amount`     DECIMAL(10,2) NOT NULL                COMMENT '实付总金额',
    `address_snapshot` TEXT          DEFAULT NULL            COMMENT '收货地址快照 JSON',
    `pay_channel`      VARCHAR(20)   DEFAULT NULL            COMMENT '支付渠道：ALIPAY/WECHAT_MOCK',
    `pay_time`         DATETIME      DEFAULT NULL            COMMENT '支付时间',
    `created_at`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
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
    `name_snapshot`  VARCHAR(200)  NOT NULL                COMMENT '商品名称快照（下单时锁定）',
    `price_snapshot` DECIMAL(10,2) NOT NULL                COMMENT '商品价格快照（下单时锁定）',
    `quantity`       INT           NOT NULL DEFAULT 1      COMMENT '购买数量',
    PRIMARY KEY (`id`),
    KEY `idx_item_order` (`order_id`),
    CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细';

-- ============================================================
-- 十、订单状态流转日志
-- ============================================================
CREATE TABLE `order_status_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id`    BIGINT       NOT NULL                COMMENT '订单 ID',
    `from_status` VARCHAR(20)  DEFAULT NULL            COMMENT '变更前状态',
    `to_status`   VARCHAR(20)  NOT NULL                COMMENT '变更后状态',
    `operator`    VARCHAR(50)  DEFAULT NULL            COMMENT '操作人（用户名或 system）',
    `reason`      VARCHAR(500) DEFAULT NULL            COMMENT '变更原因',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
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
    `channel`         VARCHAR(20)   NOT NULL                COMMENT '支付渠道：ALIPAY/WECHAT_MOCK',
    `trade_no`        VARCHAR(100)  DEFAULT NULL            COMMENT '第三方交易流水号',
    `amount`          DECIMAL(10,2) NOT NULL                COMMENT '支付金额',
    `status`          VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '支付状态：PENDING/SUCCESS/FAILED/REFUNDED',
    `notify_count`    INT           DEFAULT 0              COMMENT '回调通知次数',
    `last_notify_at`  DATETIME      DEFAULT NULL            COMMENT '最后回调时间',
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
    `oss_key`     VARCHAR(500) NOT NULL                COMMENT 'OSS 存储 Key',
    `url`         VARCHAR(500) NOT NULL                COMMENT 'OSS 访问 URL',
    `uploader_id` BIGINT       NOT NULL                COMMENT '上传者 ID',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (`id`),
    KEY `idx_file_biz_type_target` (`biz_type`, `target_id`),
    KEY `idx_file_uploader` (`uploader_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='已上传文件';

-- ============================================================
-- 十三、商家入驻申请（V2 含全部字段）
-- ============================================================
CREATE TABLE `merchant_application` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `applicant_user_id` BIGINT       NOT NULL                COMMENT '申请人用户 ID',
    `shop_name`         VARCHAR(100) NOT NULL                COMMENT '申请店铺名称',
    `reason`            TEXT         DEFAULT NULL            COMMENT '申请理由/简介',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
    `reviewer_id`       BIGINT       DEFAULT NULL            COMMENT '审核人 ID（admin 用户）',
    `reviewed_at`       DATETIME     DEFAULT NULL            COMMENT '审核时间',
    `logo_url`          VARCHAR(500) DEFAULT NULL            COMMENT '店铺 LOGO OSS URL',
    `category_id`       BIGINT       DEFAULT NULL            COMMENT '主营品类 ID',
    `description`       TEXT         DEFAULT NULL            COMMENT '店铺简介/招牌描述',
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
  `status`, `category_id`, `description`, `avg_rating`, `total_orders`,
  `business_hours_start`, `business_hours_end`, `delivery_radius`, `notice`) VALUES
(1, 'nightbb',   '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '深夜食堂',   '13800000002', '朝阳区望京SOHO T1 B1-101', 39.9963200, 116.4809000,
  'https://picsum.photos/seed/bbq-night/400/400',
  'OPEN', 1, '望京地区人气深夜食堂，主打烧烤+啤酒，现串现烤，凌晨不打烊！', 4.8, 1256,
  '18:00:00', '03:00:00', 5000, '本周五推出新品炭烤羊排，欢迎品尝！'),

(2, 'spicyhot',  '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '辣翻天小龙虾','13855555555', '朝阳区双井桥东200米', 39.8984700, 116.4675500,
  'https://picsum.photos/seed/spicy-lobster/400/400',
  'OPEN', 5, '活虾现做！麻辣鲜香！北京小龙虾排行榜TOP3，支持外卖配送', 4.6, 892,
  '16:00:00', '02:00:00', 4000, '新到一批盱眙大虾，个头更大更过瘾'),

(3, 'teahouse',   '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '老王糖水铺',  '13866666666', '海淀区五道口地铁站B口旁', 39.9929200, 116.3383200,
  'https://picsum.photos/seed/sweet-soup/400/400',
  'OPEN', 9, '30年老店，顺德传统糖水手艺传承，每一碗都是记忆中的味道', 4.9, 2100,
  '14:00:00', '23:00:00', 3500, '夏日特供冰镇系列已上线'),

(4, 'crispychick','$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '脆皮炸鸡（海淀总店）','13877778888', '海淀区中关村大街27号', 39.9836000, 116.3103000,
  'https://picsum.photos/seed/crispy-chicken/400/400',
  'OPEN', NULL, '韩式脆皮炸鸡专门店，秘制酱料10+种口味任选', 4.5, 567,
  '10:00:00', '22:00:00', 4000, '新出蜂蜜芥末口味，买一送一！'),

(5, 'hotnoodle',  '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '川味牛肉面馆', '13888889999', '朝阳区大望路万达广场B1', 39.9093000, 116.4715000,
  'https://picsum.photos/seed/noodle-soup/400/400',
  'OPEN', NULL, '正宗重庆牛肉面，大块牛肉+秘制红油，麻辣过瘾', 4.3, 340,
  '08:00:00', '21:00:00', 3000, NULL),

(6, 'teamilk',    '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '元气奶茶实验室','13866667777', '海淀区学院路15号', 39.9888000, 116.3475000,
  'https://picsum.photos/seed/bubble-tea/400/400',
  'OPEN', NULL, '年轻人的奶茶实验室，每周一款限定新品，好喝又好玩', 4.7, 1890,
  '09:00:00', '22:00:00', 5000, '本周限定：杨枝甘露波波冰'),

(7, 'pizzaking',  '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '披萨大王',    '13855556666', '东城区东直门银座B1', 39.9406000, 116.4352000,
  'https://picsum.photos/seed/pizza-king/400/400',
  'CLOSED', NULL, '18寸超大披萨，手工现做，芝士拉丝超过1米', 4.1, 210,
  '10:00:00', '21:00:00', 6000, '店铺装修中，预计8月重新开业'),

(8, 'malatang',   '$2b$10$Yx6fHpnFF8dLzLAhzhsxYeRZ1MqSQwhbWBau3AFOo7aKo02bnAWZ.',
  '杨国福麻辣烫（望京店）','13812345678', '朝阳区望京阜通东大街6号', 39.9985000, 116.4812000,
  'https://picsum.photos/seed/malatang/400/400',
  'SUSPENDED', NULL, '东北正宗麻辣烫，骨汤熬制，食材新鲜，价格实惠', 3.9, 450,
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

-- ╔══════════════════════════════════════════════════════════╗
-- ║            9. 订单 — 28 条（今天+近期）                ║
-- ║   今天=2026-07-27，18笔当天订单+10笔历史订单           ║
-- ║   营收排除 CANCELLED/REFUNDED 状态                      ║
-- ╚══════════════════════════════════════════════════════════╝
INSERT INTO `order` (`id`, `order_no`, `user_id`, `merchant_id`, `status`, `total_amount`, `address_snapshot`, `pay_channel`, `pay_time`, `created_at`) VALUES
-- ===== 今天 (2026-07-27) 的订单 — 18 笔 =====
-- 深夜食堂 (merchant 1): 4 笔
(1,  'NSP20260727001', 1,  1, 'COMPLETED',   52.00,  '{"receiver":"测试员","phone":"13800000003","detail":"朝阳区望京SOHO T1-1201"}', 'WECHAT_MOCK', '2026-07-27 21:05:00', '2026-07-27 21:00:00'),
(2,  'NSP20260727002', 2,  1, 'PAID',        28.00,  '{"receiver":"张三","phone":"13811111111","detail":"朝阳区三里屯SOHO 5号楼"}', 'ALIPAY',      '2026-07-27 20:30:00', '2026-07-27 20:25:00'),
(3,  'NSP20260727003', 6,  1, 'COOKING',     36.00,  '{"receiver":"孙七","phone":"13855551111","detail":"海淀区五道口华清嘉园7-1202"}', 'WECHAT_MOCK', '2026-07-27 19:10:00', '2026-07-27 19:05:00'),
(4,  'NSP20260727004', 3,  1, 'PENDING_PAY', 18.00,  '{"receiver":"李四","phone":"13822222222","detail":"东城区王府井大街138号"}', NULL, NULL, '2026-07-27 22:30:00'),
-- 辣翻天小龙虾 (merchant 2): 3 笔
(5,  'NSP20260727005', 1,  2, 'COMPLETED',  156.00,  '{"receiver":"测试员","phone":"13800000003","detail":"朝阳区望京SOHO T1-1201"}', 'ALIPAY',      '2026-07-27 19:30:00', '2026-07-27 19:20:00'),
(6,  'NSP20260727006', 7,  2, 'ACCEPTED',    88.00,  '{"receiver":"周八","phone":"13866661112","detail":"朝阳区国贸建外SOHO 9-301"}', 'WECHAT_MOCK', '2026-07-27 20:00:00', '2026-07-27 19:55:00'),
(7,  'NSP20260727007', 4,  2, 'PAID',        68.00,  '{"receiver":"王五","phone":"13833333333","detail":"西城区金融街7号"}', 'ALIPAY',      '2026-07-27 21:40:00', '2026-07-27 21:35:00'),
-- 老王糖水铺 (merchant 3): 5 笔
(8,  'NSP20260727008', 2,  3, 'COMPLETED',   26.00,  '{"receiver":"张三","phone":"13811111111","detail":"朝阳区三里屯SOHO 5号楼"}', 'WECHAT_MOCK', '2026-07-27 15:10:00', '2026-07-27 15:00:00'),
(9,  'NSP20260727009', 6,  3, 'COMPLETED',   18.00,  '{"receiver":"孙七","phone":"13855551111","detail":"海淀区五道口华清嘉园7-1202"}', 'WECHAT_MOCK', '2026-07-27 16:30:00', '2026-07-27 16:25:00'),
(10, 'NSP20260727010', 1,  3, 'PAID',        40.00,  '{"receiver":"测试员","phone":"13800000003","detail":"朝阳区望京SOHO T1-1201"}', 'ALIPAY',      '2026-07-27 17:00:00', '2026-07-27 16:55:00'),
(11, 'NSP20260727011', 8,  3, 'COOKING',     32.00,  '{"receiver":"吴九","phone":"13877771113","detail":"丰台区方庄芳城园一区12-401"}', 'WECHAT_MOCK', '2026-07-27 18:20:00', '2026-07-27 18:15:00'),
(12, 'NSP20260727012', 12, 3, 'ACCEPTED',    22.00,  '{"receiver":"曹三","phone":"13811111117","detail":"东城区鼓楼东大街12号"}', 'ALIPAY',      '2026-07-27 21:00:00', '2026-07-27 20:55:00'),
-- 脆皮炸鸡 (merchant 4): 2 笔
(13, 'NSP20260727013', 9,  4, 'COMPLETED',   48.00,  '{"receiver":"郑十","phone":"13888881114","detail":"通州区万达广场B座2105"}', 'WECHAT_MOCK', '2026-07-27 12:30:00', '2026-07-27 12:20:00'),
(14, 'NSP20260727014', 10, 4, 'PAID',        32.00,  '{"receiver":"钱一","phone":"13899991115","detail":"大兴区亦庄荣华中路8号院3-102"}', 'ALIPAY',      '2026-07-27 13:40:00', '2026-07-27 13:35:00'),
-- 川味牛肉面馆 (merchant 5): 1 笔
(15, 'NSP20260727015', 11, 5, 'PAID',        22.00,  '{"receiver":"刘二","phone":"13800001116","detail":"海淀区知春路56号"}', 'WECHAT_MOCK', '2026-07-27 11:00:00', '2026-07-27 10:55:00'),
-- 元气奶茶实验室 (merchant 6): 3 笔
(16, 'NSP20260727016', 2,  6, 'COMPLETED',   36.00,  '{"receiver":"张三","phone":"13811111111","detail":"朝阳区三里屯SOHO 5号楼"}', 'ALIPAY',      '2026-07-27 14:30:00', '2026-07-27 14:20:00'),
(17, 'NSP20260727017', 6,  6, 'PAID',        18.00,  '{"receiver":"孙七","phone":"13855551111","detail":"海淀区五道口华清嘉园7-1202"}', 'WECHAT_MOCK', '2026-07-27 15:50:00', '2026-07-27 15:45:00'),
(18, 'NSP20260727018', 7,  6, 'ACCEPTED',    52.00,  '{"receiver":"周八","phone":"13866661112","detail":"朝阳区国贸建外SOHO 9-301"}', 'ALIPAY',      '2026-07-27 16:10:00', '2026-07-27 16:00:00'),

-- ===== 历史订单 (2026-07-20 ~ 07-26) — 10 笔 =====
(19, 'NSP20260726001', 1,  1, 'COMPLETED',   46.00,  '{"receiver":"测试员","phone":"13800000003","detail":"朝阳区望京SOHO T1-1201"}', 'ALIPAY',      '2026-07-26 21:00:00', '2026-07-26 20:50:00'),
(20, 'NSP20260725001', 3,  2, 'COMPLETED',  128.00,  '{"receiver":"李四","phone":"13822222222","detail":"东城区王府井大街138号"}', 'WECHAT_MOCK', '2026-07-25 19:00:00', '2026-07-25 18:50:00'),
(21, 'NSP20260724001', 4,  3, 'CANCELLED',   16.00,  '{"receiver":"王五","phone":"13833333333","detail":"西城区金融街7号"}', NULL, NULL, '2026-07-24 10:00:00'),
(22, 'NSP20260723001', 5,  1, 'COMPLETED',   35.00,  '{"receiver":"赵六","phone":"13844444444","detail":"东城区东直门银座B1"}', 'WECHAT_MOCK', '2026-07-23 22:00:00', '2026-07-23 21:50:00'),
(23, 'NSP20260722001', 8,  4, 'COMPLETED',   28.00,  '{"receiver":"吴九","phone":"13877771113","detail":"丰台区方庄芳城园一区12-401"}', 'ALIPAY',      '2026-07-22 12:00:00', '2026-07-22 11:50:00'),
(24, 'NSP20260721001', 9,  5, 'REFUNDING',   20.00,  '{"receiver":"郑十","phone":"13888881114","detail":"通州区万达广场B座2105"}', 'ALIPAY',      '2026-07-21 13:00:00', '2026-07-21 12:50:00'),
(25, 'NSP20260720001', 10, 6, 'COMPLETED',   22.00,  '{"receiver":"钱一","phone":"13899991115","detail":"大兴区亦庄荣华中路8号院3-102"}', 'WECHAT_MOCK', '2026-07-20 15:00:00', '2026-07-20 14:50:00'),
(26, 'NSP20260720002', 6,  6, 'REFUNDED',    40.00,  '{"receiver":"孙七","phone":"13855551111","detail":"海淀区五道口华清嘉园7-1202"}', 'ALIPAY',      '2026-07-20 18:00:00', '2026-07-20 17:50:00'),
(27, 'NSP20260726002', 12, 7, 'COMPLETED',   68.00,  '{"receiver":"曹三","phone":"13811111117","detail":"东城区鼓楼东大街12号"}', 'ALIPAY',      '2026-07-26 13:00:00', '2026-07-26 12:50:00'),
(28, 'NSP20260725002', 13, 8, 'COMPLETED',   26.00,  '{"receiver":"魏四","phone":"13822221118","detail":"海淀区知春路56号"}', 'WECHAT_MOCK', '2026-07-25 12:00:00', '2026-07-25 11:50:00');

-- ───── 10. 订单明细 ─────
INSERT INTO `order_item` (`id`, `order_id`, `product_id`, `spec_id`, `name_snapshot`, `price_snapshot`, `quantity`) VALUES
-- 订单1: 深夜食堂 COMPLETED 52.00
(1,  1, 2,  4,  '牛肉串-大份(10串)',     9.50,  4),
(2,  1, 7,  12, '冰镇酸梅汤-壶(1.5L)',   14.00, 1),
-- 订单2: 深夜食堂 PAID 28.00
(3,  2, 1,  1,  '羊肉串-小份(5串)',      4.00,  3),
(4,  2, 3,  5,  '烤鸡翅-3只装',           8.00,  2),
-- 订单3: 深夜食堂 COOKING 36.00
(5,  3, 4,  7,  '烤茄子-标准份',         10.00,  1),
(6,  3, 5,  8,  '烤鱿鱼-1只',           12.00,  1),
(7,  3, 2,  4,  '牛肉串-大份(10串)',     9.50,  1),
(8,  3, 7,  11, '冰镇酸梅汤-杯(500ml)',   4.50,  1),
-- 订单4: 深夜食堂 PENDING_PAY 18.00
(9,  4, 1,  2,  '羊肉串-大份(10串)',     7.50,  2),
(10, 4, 11, 19, '烤馒头-1个',            3.00,  1),
-- 订单5: 辣翻天 COMPLETED 156.00
(11, 5, 16, 28, '麻辣小龙虾(大份)-微辣',128.00,  1),
(12, 5, 24, 39, '盐水毛豆-标准份',       10.00,  1),
(13, 5, 22, 37, '精酿扎啤-1杯',         18.00,  1),
-- 订单6: 辣翻天 ACCEPTED 88.00
(14, 6, 19, 33, '香辣烤鱼-微辣',         88.00,  1),
-- 订单7: 辣翻天 PAID 68.00
(15, 7, 15, 25, '麻辣小龙虾(小份)-微辣', 68.00,  1),
-- 订单8: 老王糖水铺 COMPLETED 26.00
(16, 8, 28, 46, '杨枝甘露-标准杯',       16.00,  1),
(17, 8, 32, 53, '咖喱鱼蛋-小份(6粒)',    10.00,  1),
-- 订单9: 老王糖水铺 COMPLETED 18.00
(18, 9, 26, 43, '绿豆沙-冰饮',            8.00,  1),
(19, 9, 31, 51, '鸡蛋仔-原味',           10.00,  1),
-- 订单10: 老王糖水铺 PAID 40.00
(20, 10, 28, 46, '杨枝甘露-标准杯',      16.00,  2),
(21, 10, 31, 52, '鸡蛋仔-巧克力味',       8.00,  1),
-- 订单11: 老王糖水铺 COOKING 32.00
(22, 11, 27, 45, '双皮奶-加红豆',        14.00,  1),
(23, 11, 25, 41, '红豆沙-冰饮',          10.00,  1),
(24, 11, 32, 53, '咖喱鱼蛋-小份(6粒)',    8.00,  1),
-- 订单12: 老王糖水铺 ACCEPTED 22.00
(25, 12, 29, 48, '鲜榨西瓜汁-大杯',      14.00,  1),
(26, 12, 31, 51, '鸡蛋仔-原味',           8.00,  1),
-- 订单13: 脆皮炸鸡 COMPLETED 48.00
(27, 13, 34, 56, '原味脆皮炸鸡-整只',    48.00,  1),
-- 订单14: 脆皮炸鸡 PAID 32.00
(28, 14, 35, 57, '韩式甜辣炸鸡-标准份',  32.00,  1),
-- 订单15: 川味牛肉面馆 PAID 22.00
(29, 15, 40, 62, '招牌牛肉面-标准份',    22.00,  1),
-- 订单16: 元气奶茶 COMPLETED 36.00
(30, 16, 49, 73, '多肉葡萄-标准杯',      22.00,  1),
(31, 16, 46, 69, '经典珍珠奶茶-中杯',    14.00,  1),
-- 订单17: 元气奶茶 PAID 18.00
(32, 17, 51, 75, '黑糖脏脏茶-标准杯',    18.00,  1),
-- 订单18: 元气奶茶 ACCEPTED 52.00
(33, 18, 49, 73, '多肉葡萄-标准杯',      22.00,  2),
(34, 18, 50, 74, '芝士奶盖绿茶-标准杯',   8.00,  1),
-- 订单19: 深夜食堂 COMPLETED 46.00 (历史)
(35, 19, 2,  4,  '牛肉串-大份(10串)',     9.50,  3),
(36, 19, 4,  7,  '烤茄子-标准份',         10.00,  1),
(37, 19, 9,  15, '冰可乐-1罐',            4.00,  2),
-- 订单20: 辣翻天 COMPLETED 128.00 (历史)
(38, 20, 16, 28, '麻辣小龙虾(大份)-微辣',128.00,  1),
-- 订单21: 老王糖水铺 CANCELLED 16.00 (历史)
(39, 21, 28, 46, '杨枝甘露-标准杯',       16.00,  1),
-- 订单22: 深夜食堂 COMPLETED 35.00 (历史)
(40, 22, 10, 17, '炒方便面-标配(加蛋)',   10.00,  2),
(41, 22, 3,  6,  '烤鸡翅-6只装',          15.00,  1),
-- 订单23: 脆皮炸鸡 COMPLETED 28.00 (历史)
(42, 23, 33, 55, '原味脆皮炸鸡-半只',    28.00,  1),
-- 订单24: 川味牛肉面馆 REFUNDING 20.00 (历史)
(43, 24, 41, 64, '红烧肥肠面-标准份',    20.00,  1),
-- 订单25: 元气奶茶 COMPLETED 22.00 (历史)
(44, 25, 49, 73, '多肉葡萄-标准杯',      22.00,  1),
-- 订单26: 元气奶茶 REFUNDED 40.00 (历史)
(45, 26, 49, 73, '多肉葡萄-标准杯',      22.00,  1),
(46, 26, 51, 75, '黑糖脏脏茶-标准杯',    18.00,  1),
-- 订单27: 披萨大王 COMPLETED 68.00 (历史)
(47, 27, 52, 76, '超级至尊披萨-12寸',    68.00,  1),
-- 订单28: 杨国福麻辣烫 COMPLETED 26.00 (历史)
(48, 28, 58, 84, '自选麻辣烫(大份)-微辣',26.00,  1);

-- ───── 11. 订单状态流转日志 ─────
INSERT INTO `order_status_log` (`id`, `order_id`, `from_status`, `to_status`, `operator`, `reason`) VALUES
-- 订单1: 深夜食堂 COMPLETED
(1,  1,  NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(2,  1,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(3,  1,  'PAID',        'ACCEPTED',    'nightbb',  '商家接单'),
(4,  1,  'ACCEPTED',    'COOKING',     'nightbb',  '开始制作'),
(5,  1,  'COOKING',     'COMPLETED',   'nightbb',  '制作完成'),
-- 订单2: 深夜食堂 PAID
(6,  2,  NULL,          'PENDING_PAY', 'zhangsan', '用户下单'),
(7,  2,  'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
-- 订单3: 深夜食堂 COOKING
(8,  3,  NULL,          'PENDING_PAY', 'sunqi',    '用户下单'),
(9,  3,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(10, 3,  'PAID',        'ACCEPTED',    'nightbb',  '商家接单'),
(11, 3,  'ACCEPTED',    'COOKING',     'nightbb',  '开始制作'),
-- 订单4: 深夜食堂 PENDING_PAY
(12, 4,  NULL,          'PENDING_PAY', 'lisi',     '用户下单'),
-- 订单5: 辣翻天 COMPLETED
(13, 5,  NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(14, 5,  'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(15, 5,  'PAID',        'ACCEPTED',    'spicyhot', '商家接单'),
(16, 5,  'ACCEPTED',    'COOKING',     'spicyhot', '开始制作'),
(17, 5,  'COOKING',     'COMPLETED',   'spicyhot', '制作完成'),
-- 订单6: 辣翻天 ACCEPTED
(18, 6,  NULL,          'PENDING_PAY', 'zhouba',   '用户下单'),
(19, 6,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(20, 6,  'PAID',        'ACCEPTED',    'spicyhot', '商家接单'),
-- 订单7: 辣翻天 PAID
(21, 7,  NULL,          'PENDING_PAY', 'wangwu',   '用户下单'),
(22, 7,  'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
-- 订单8: 老王糖水铺 COMPLETED
(23, 8,  NULL,          'PENDING_PAY', 'zhangsan', '用户下单'),
(24, 8,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(25, 8,  'PAID',        'ACCEPTED',    'teahouse', '商家接单'),
(26, 8,  'ACCEPTED',    'COOKING',     'teahouse', '开始制作'),
(27, 8,  'COOKING',     'COMPLETED',   'teahouse', '制作完成'),
-- 订单9: 老王糖水铺 COMPLETED
(28, 9,  NULL,          'PENDING_PAY', 'sunqi',    '用户下单'),
(29, 9,  'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(30, 9,  'PAID',        'ACCEPTED',    'teahouse', '商家接单'),
(31, 9,  'ACCEPTED',    'COOKING',     'teahouse', '开始制作'),
(32, 9,  'COOKING',     'COMPLETED',   'teahouse', '制作完成'),
-- 订单10: 老王糖水铺 PAID
(33, 10, NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(34, 10, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
-- 订单11: 老王糖水铺 COOKING
(35, 11, NULL,          'PENDING_PAY', 'wujiu',    '用户下单'),
(36, 11, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(37, 11, 'PAID',        'ACCEPTED',    'teahouse', '商家接单'),
(38, 11, 'ACCEPTED',    'COOKING',     'teahouse', '开始制作'),
-- 订单12: 老王糖水铺 ACCEPTED
(39, 12, NULL,          'PENDING_PAY', 'caosan',   '用户下单'),
(40, 12, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(41, 12, 'PAID',        'ACCEPTED',    'teahouse', '商家接单'),
-- 订单13: 脆皮炸鸡 COMPLETED
(42, 13, NULL,          'PENDING_PAY', 'zhengshi', '用户下单'),
(43, 13, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(44, 13, 'PAID',        'ACCEPTED',    'crispychick','商家接单'),
(45, 13, 'ACCEPTED',    'COOKING',     'crispychick','开始制作'),
(46, 13, 'COOKING',     'COMPLETED',   'crispychick','制作完成'),
-- 订单14: 脆皮炸鸡 PAID
(47, 14, NULL,          'PENDING_PAY', 'qianyi',   '用户下单'),
(48, 14, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
-- 订单15: 川味牛肉面馆 PAID
(49, 15, NULL,          'PENDING_PAY', 'liuer',    '用户下单'),
(50, 15, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
-- 订单16: 元气奶茶 COMPLETED
(51, 16, NULL,          'PENDING_PAY', 'zhangsan', '用户下单'),
(52, 16, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(53, 16, 'PAID',        'ACCEPTED',    'teamilk',  '商家接单'),
(54, 16, 'ACCEPTED',    'COOKING',     'teamilk',  '开始制作'),
(55, 16, 'COOKING',     'COMPLETED',   'teamilk',  '制作完成'),
-- 订单17: 元气奶茶 PAID
(56, 17, NULL,          'PENDING_PAY', 'sunqi',    '用户下单'),
(57, 17, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
-- 订单18: 元气奶茶 ACCEPTED
(58, 18, NULL,          'PENDING_PAY', 'zhouba',   '用户下单'),
(59, 18, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(60, 18, 'PAID',        'ACCEPTED',    'teamilk',  '商家接单'),
-- 历史订单
(61, 19, NULL,          'PENDING_PAY', 'tester',   '用户下单'),
(62, 19, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(63, 19, 'PAID',        'ACCEPTED',    'nightbb',  '商家接单'),
(64, 19, 'ACCEPTED',    'COOKING',     'nightbb',  '开始制作'),
(65, 19, 'COOKING',     'COMPLETED',   'nightbb',  '制作完成'),
(66, 20, NULL,          'PENDING_PAY', 'lisi',     '用户下单'),
(67, 20, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(68, 20, 'PAID',        'ACCEPTED',    'spicyhot', '商家接单'),
(69, 20, 'ACCEPTED',    'COOKING',     'spicyhot', '开始制作'),
(70, 20, 'COOKING',     'COMPLETED',   'spicyhot', '制作完成'),
(71, 21, NULL,          'PENDING_PAY', 'wangwu',   '用户下单'),
(72, 21, 'PENDING_PAY', 'CANCELLED',   'wangwu',   '用户取消订单'),
(73, 22, NULL,          'PENDING_PAY', 'zhaoliu',  '用户下单'),
(74, 22, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(75, 22, 'PAID',        'ACCEPTED',    'nightbb',  '商家接单'),
(76, 22, 'ACCEPTED',    'COOKING',     'nightbb',  '开始制作'),
(77, 22, 'COOKING',     'COMPLETED',   'nightbb',  '制作完成'),
(78, 23, NULL,          'PENDING_PAY', 'wujiu',    '用户下单'),
(79, 23, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(80, 23, 'PAID',        'ACCEPTED',    'crispychick','商家接单'),
(81, 23, 'ACCEPTED',    'COOKING',     'crispychick','开始制作'),
(82, 23, 'COOKING',     'COMPLETED',   'crispychick','制作完成'),
(83, 24, NULL,          'PENDING_PAY', 'zhengshi', '用户下单'),
(84, 24, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(85, 24, 'PAID',        'REFUNDING',   'zhengshi', '用户申请退款'),
(86, 25, NULL,          'PENDING_PAY', 'qianyi',   '用户下单'),
(87, 25, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(88, 25, 'PAID',        'ACCEPTED',    'teamilk',  '商家接单'),
(89, 25, 'ACCEPTED',    'COOKING',     'teamilk',  '开始制作'),
(90, 25, 'COOKING',     'COMPLETED',   'teamilk',  '制作完成'),
(91, 26, NULL,          'PENDING_PAY', 'sunqi',    '用户下单'),
(92, 26, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(93, 26, 'PAID',        'REFUNDING',   'sunqi',    '用户申请退款'),
(94, 26, 'REFUNDING',   'REFUNDED',    'system',   '退款到账'),
(95, 27, NULL,          'PENDING_PAY', 'caosan',   '用户下单'),
(96, 27, 'PENDING_PAY', 'PAID',        'system',   '支付宝支付回调'),
(97, 27, 'PAID',        'ACCEPTED',    'pizzaking', '商家接单'),
(98, 27, 'ACCEPTED',    'COOKING',     'pizzaking', '开始制作'),
(99, 27, 'COOKING',     'COMPLETED',   'pizzaking', '制作完成'),
(100,28, NULL,          'PENDING_PAY', 'weisi',    '用户下单'),
(101,28, 'PENDING_PAY', 'PAID',        'system',   '微信支付回调'),
(102,28, 'PAID',        'ACCEPTED',    'malatang', '商家接单'),
(103,28, 'ACCEPTED',    'COOKING',     'malatang', '开始制作'),
(104,28, 'COOKING',     'COMPLETED',   'malatang', '制作完成');

-- ───── 12. 支付记录 ─────
INSERT INTO `payment_record` (`id`, `order_no`, `channel`, `trade_no`, `amount`, `status`) VALUES
-- 今天已支付的订单
(1,  'NSP20260727001', 'WECHAT_MOCK', 'WM20260727001', 52.00,  'SUCCESS'),
(2,  'NSP20260727002', 'ALIPAY',      'AL20260727002', 28.00,  'SUCCESS'),
(3,  'NSP20260727003', 'WECHAT_MOCK', 'WM20260727003', 36.00,  'SUCCESS'),
(4,  'NSP20260727005', 'ALIPAY',      'AL20260727005', 156.00, 'SUCCESS'),
(5,  'NSP20260727006', 'WECHAT_MOCK', 'WM20260727006', 88.00,  'SUCCESS'),
(6,  'NSP20260727007', 'ALIPAY',      'AL20260727007', 68.00,  'SUCCESS'),
(7,  'NSP20260727008', 'WECHAT_MOCK', 'WM20260727008', 26.00,  'SUCCESS'),
(8,  'NSP20260727009', 'WECHAT_MOCK', 'WM20260727009', 18.00,  'SUCCESS'),
(9,  'NSP20260727010', 'ALIPAY',      'AL20260727010', 40.00,  'SUCCESS'),
(10, 'NSP20260727011', 'WECHAT_MOCK', 'WM20260727011', 32.00,  'SUCCESS'),
(11, 'NSP20260727012', 'ALIPAY',      'AL20260727012', 22.00,  'SUCCESS'),
(12, 'NSP20260727013', 'WECHAT_MOCK', 'WM20260727013', 48.00,  'SUCCESS'),
(13, 'NSP20260727014', 'ALIPAY',      'AL20260727014', 32.00,  'SUCCESS'),
(14, 'NSP20260727015', 'WECHAT_MOCK', 'WM20260727015', 22.00,  'SUCCESS'),
(15, 'NSP20260727016', 'ALIPAY',      'AL20260727016', 36.00,  'SUCCESS'),
(16, 'NSP20260727017', 'WECHAT_MOCK', 'WM20260727017', 18.00,  'SUCCESS'),
(17, 'NSP20260727018', 'ALIPAY',      'AL20260727018', 52.00,  'SUCCESS'),
-- 历史订单的支付记录
(18, 'NSP20260726001', 'ALIPAY',      'AL20260726001', 46.00,  'SUCCESS'),
(19, 'NSP20260725001', 'WECHAT_MOCK', 'WM20260725001', 128.00, 'SUCCESS'),
(20, 'NSP20260723001', 'WECHAT_MOCK', 'WM20260723001', 35.00,  'SUCCESS'),
(21, 'NSP20260722001', 'ALIPAY',      'AL20260722001', 28.00,  'SUCCESS'),
(22, 'NSP20260721001', 'ALIPAY',      'AL20260721001', 20.00,  'SUCCESS'),
(23, 'NSP20260720001', 'WECHAT_MOCK', 'WM20260720001', 22.00,  'SUCCESS'),
(24, 'NSP20260720002', 'ALIPAY',      'AL20260720002', 40.00,  'SUCCESS'),
(25, 'NSP20260726002', 'ALIPAY',      'AL20260726002', 68.00,  'SUCCESS'),
(26, 'NSP20260725002', 'WECHAT_MOCK', 'WM20260725002', 26.00,  'SUCCESS');
