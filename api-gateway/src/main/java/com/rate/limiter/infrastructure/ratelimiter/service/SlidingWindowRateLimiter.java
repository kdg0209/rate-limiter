package com.rate.limiter.infrastructure.ratelimiter.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import com.rate.limiter.infrastructure.ratelimiter.dto.RateResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public final class SlidingWindowRateLimiter implements RateLimiter{

	private static final String SORTED_SET_MIN_VALUE = "0";

	// lua script
	private static final String SLIDING_WINDOW_LUA_SCRIPT = "sliding-window.lua";

	// 남은 요청 수
	private static final String REMAINING_HEADER = " x-ratelimit-remaining";

	// 초당 몇개를 허용하는지 개수
	private static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";

	private final ReactiveRedisTemplate<String, String> template;
	private final RedisScript<Long> script = new DefaultRedisScript<>(getLuaScript(), Long.class);

	/**
	 * 해당 메서드는 제공된 키(상품 번호)를 기반으로 요청이 허용되는지 여부를 확인하고 RateResponse를 반환합니다.
	 * 1초당 최대 접근 가능한 횟수는 5회입니다.
	 */
	@Override
	public Mono<RateResponse> isAllowed(Long productId) {
		var keys = settingKey(productId);
		var args = settingArgs();

		return template.execute(script, keys, args)
			.next()
			.map(count -> {
				var allowed = count <= Config.MAX_REQUESTS;   		// 허용 가능 여부
				var remainingCount = Config.MAX_REQUESTS - count; 	// 남은 횟수

				log.info("allowed:{}, remainingCount={}", allowed, remainingCount);
				return new RateResponse(allowed, count, getHeaders(remainingCount));
			})
			.defaultIfEmpty(RateResponse.access());
	}

	private List<String> settingKey(Long productId) {
		return Collections.singletonList(getKey(productId));
	}

	private List<String> settingArgs() {
		var currentTimeMillis = Instant.now().toEpochMilli();
		var windowSizeInMillis = Config.EXPIRE_TIME_MS;
		var ttl = Config.EXPIRE_TIME_MS / 1000; // 1ms

		return Arrays.asList(
			SORTED_SET_MIN_VALUE, // ARGV[1]: min
			String.valueOf(currentTimeMillis - windowSizeInMillis), // ARGV[2]: max
			String.valueOf(currentTimeMillis), // ARGV[3]: score
			String.valueOf(currentTimeMillis), // ARGV[4]: value
			String.valueOf(ttl) // ARGV[5]: ttl
		);
	}

	private String getLuaScript() {
		var resource = new ClassPathResource(SLIDING_WINDOW_LUA_SCRIPT);

		try {
			return new String(Files.readAllBytes(Paths.get(resource.getURI())));
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

	private String getKey(Long productId) {
		return String.format("product:%s", productId);
	}

	private Map<String, String> getHeaders(long remainingCount) {
		Map<String, String> headers = new HashMap<>();
		headers.put(REMAINING_HEADER, String.valueOf(remainingCount));
		headers.put(REPLENISH_RATE_HEADER, String.valueOf(Config.MAX_REQUESTS));
		return headers;
	}

	static class Config {

		static final int MAX_REQUESTS = 5;
		static final long EXPIRE_TIME_MS = 1000; // 1ms
	}
}
