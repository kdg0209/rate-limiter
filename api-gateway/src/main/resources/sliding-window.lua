local key = KEYS[1]
local min = ARGV[1]
local max = ARGV[2]
local score = ARGV[3]
local value = ARGV[4]
local ttl = ARGV[5]

redis.call('ZREMRANGEBYSCORE', key, min, max)
redis.call('ZADD', key, score, value)
redis.call('EXPIRE', key, ttl)

redis.log(redis.LOG_WARNING, "ZREMRANGEBYSCORE command key:", key, ", min:", min, ", max:", max)
redis.log(redis.LOG_WARNING, "ZADD command key:", key, ", score:", score, ", value:", value)
redis.log(redis.LOG_WARNING, "EXPIRE command key:", key, ", ttl:", ttl)
redis.log(redis.LOG_WARNING, "ZCARD command key:", key, ", cardinality", redis.call('ZCARD', KEYS[1]))

return redis.call('ZCARD', key)