### 아키텍처

<img width="1032" alt="스크린샷 2024-06-22 오후 3 04 07" src="https://github.com/kdg0209/rate-limiter/assets/80187200/d35b7a03-f0af-4b8d-a77d-d2e62a3cd7e9">

<br><br>

### 적용한 처리율 제한기 알고리즘

- Sliding Window Log

<br>

### 흐름

- 사용자가 product에 접근하는 경우 redis의 sorted set에 사용자의 key(ex: product:2)와 타임스탬프를 저장하게 됩니다.
- 1초 이내에 5번 이상 접근하려는 경우 Too many requests 예외가 발생하게 됩니다.
- redis의 sorted set의 ttl은 1초 입니다.

<br>

#### Lua Script

```script
local key = KEYS[1]
local min = ARGV[1]
local max = ARGV[2]
local score = ARGV[3]
local value = ARGV[4]
local ttl = ARGV[5]

redis.call('ZREMRANGEBYSCORE', key, min, max)
redis.call('ZADD', key, score, value)
redis.call('EXPIRE', key, ttl)
local cardinality = redis.call('ZCARD', KEYS[1])

redis.log(redis.LOG_WARNING, "ZREMRANGEBYSCORE command key:", key, ", min:", min, ", max:", max)
redis.log(redis.LOG_WARNING, "ZADD command key:", key, ", score:", score, ", value:", value)
redis.log(redis.LOG_WARNING, "EXPIRE command key:", key, ", ttl:", ttl)
redis.log(redis.LOG_WARNING, "ZCARD command key:", key, ", cardinality", cardinality)

return cardinality
```

<br>

### 처리율 제한기 알고리즘 종류

- https://github.com/kdg0209/realizers/blob/main/%EA%B0%80%EC%83%81%20%EB%A9%B4%EC%A0%91%20%EC%82%AC%EB%A1%80%EB%A1%9C%20%EB%B0%B0%EC%9A%B0%EB%8A%94%20%EB%8C%80%EA%B7%9C%EB%AA%A8%20%EC%8B%9C%EC%8A%A4%ED%85%9C%20%EC%84%A4%EA%B3%84%20%EA%B8%B0%EC%B4%88%201/04%EC%9E%A5%20%EC%B2%98%EB%A6%AC%EC%9C%A8%20%EC%A0%9C%ED%95%9C%20%EC%9E%A5%EC%B9%98%EC%9D%98%20%EC%84%A4%EA%B3%84.md

<br>

### 참고

- https://medium.com/@mbanaee61/efficient-rate-limiting-in-reactive-spring-boot-applications-with-redis-and-junit-testing-20675e73104a
