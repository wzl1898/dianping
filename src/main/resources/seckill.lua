-- 秒杀资格预检脚本
-- KEYS[1] = seckill:stock:{voucherId}
-- KEYS[2] = seckill:order:{voucherId}
-- ARGV[1] = userId

-- 1. 判断库存是否充足
if (tonumber(redis.call('get', KEYS[1])) or 0) <= 0 then
    return 1  -- 库存不足
end

-- 2. 判断用户是否已秒杀过
if redis.call('sismember', KEYS[2], ARGV[1]) == 1 then
    return 2  -- 重复下单
end

-- 3. 扣减库存，记录用户
redis.call('decr', KEYS[1])
redis.call('sadd', KEYS[2], ARGV[1])

return 0  -- 秒杀资格获取成功
