-- ============================================================
-- V2__seed_data.sql — 开发环境种子数据
-- 启动后自动执行，提供可立即测试的商家/品类/商品/规格
--
-- 测试用户请通过 API 注册：
--   POST /api/user/register  { "username":"admin",   "password":"admin123", "phone":"13800000001" }  → 注册后手动改 role=ADMIN
--   POST /api/user/register  { "username":"nightbb", "password":"m123456",  "phone":"13800000002" }  → 注册后在 DB 改 role=MERCHANT
--   POST /api/user/register  { "username":"tester",  "password":"test123",  "phone":"13800000003" }
--
-- 商家店主关联：注册 nightbb 用户后，将 merchant.owner_user_id 更新为对应 user.id
-- ============================================================

-- ───── 1. 测试商家（北京望京 SOHO，店主 user_id 需手动更新） ─────
INSERT IGNORE INTO `merchant` (`id`, `name`, `owner_user_id`, `phone`, `address`, `lat`, `lng`, `avatar_url`, `status`, `business_hours_start`, `business_hours_end`, `delivery_radius`)
VALUES
(1, '深夜食堂', 2, '13800000002', '北京市朝阳区望京SOHO T1', 39.99632, 116.48090, '', 'OPEN', '18:00:00', '03:00:00', 5000);

-- ───── 2. 测试品类 ─────
INSERT IGNORE INTO `category` (`id`, `merchant_id`, `name`, `sort`) VALUES
(1, 1, '烧烤类', 1),
(2, 1, '饮品类', 2),
(3, 1, '主食类', 3);

-- ───── 3. 测试商品 ─────
INSERT IGNORE INTO `product` (`id`, `merchant_id`, `category_id`, `name`, `price`, `image_url`, `status`, `description`) VALUES
(1, 1, 1, '羊肉串',      4.00,  '', 'ON', '新鲜羊肉，现串现烤'),
(2, 1, 1, '牛肉串',      5.00,  '', 'ON', '秘制腌料，外焦里嫩'),
(3, 1, 1, '烤鸡翅',      8.00,  '', 'ON', '奥尔良风味，3只/份'),
(4, 1, 2, '冰镇酸梅汤',  6.00,  '', 'ON', '自制酸梅汤，解腻解辣'),
(5, 1, 2, '精酿啤酒',    12.00, '', 'ON', '本地精酿，500ml'),
(6, 1, 3, '炒方便面',    10.00, '', 'ON', '夜市经典，加蛋加肠'),
(7, 1, 3, '烤馒头',      3.00,  '', 'ON', '外酥里软，配炼乳'),
(8, 1, 1, '烤茄子',      10.00, '', 'ON', '蒜蓉烤茄子，夜市王牌');

-- ───── 4. 测试规格（stock 同步到 Redis 后生效，否则下单时自动初始化） ─────
INSERT IGNORE INTO `product_spec` (`id`, `product_id`, `name`, `price_delta`, `stock`) VALUES
( 1, 1, '小份（5串）',    0.00,  100),
( 2, 1, '大份（10串）',   3.50,  80),
( 3, 2, '小份（5串）',    0.00,  100),
( 4, 2, '大份（10串）',   4.50,  80),
( 5, 3, '3只装',          0.00,  50),
( 6, 3, '6只装',          7.00,  40),
( 7, 4, '杯（500ml）',    0.00,  100),
( 8, 4, '壶（1.5L）',     8.00,  40),
( 9, 5, '单瓶（500ml）',  0.00,  200),
(10, 5, '一打（12瓶）',   100.00, 30),
(11, 6, '标配（加蛋）',    0.00,  60),
(12, 6, '豪华（加蛋加肠）', 4.00, 50),
(13, 7, '1个',            0.00,  100),
(14, 7, '3个装',          -0.50, 80),
(15, 8, '标准份',          0.00,  60);
