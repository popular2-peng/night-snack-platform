-- ============================================================
-- stock_deduct.lua — 库存原子扣减
-- 保证 Redis 库存操作的原子性，禁止 Java 先读后写
--
-- KEYS[1] = Redis stock key (product:stock:{specId})
-- ARGV[1] = quantity to deduct
--
-- 返回值:
--   0   = 扣减成功
--   -1  = 库存不足
--   -2  = 库存 Key 不存在（未初始化）
-- ============================================================

local key = KEYS[1]
local qty = tonumber(ARGV[1])

local stock = redis.call('GET', key)

if stock == false then
    return -2
end

stock = tonumber(stock)

if stock >= qty then
    redis.call('DECRBY', key, qty)
    return 0
else
    return -1
end
