package com.rate.limiter.infrastructure.ratelimiter.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
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

	// lua script
	private static final String SLIDING_WINDOW_LUA_SCRIPT = "sliding-window.lua";

	// 남은 요청 수
	private static final String REMAINING_HEADER = " x-ratelimit-remaining";
	private static final String REPLENISH_RATE_HEADER = "X-RateLimit-Replenish-Rate";
	private static final String BURST_CAPACITY_HEADER = "X-RateLimit-Burst-Capacity";
	private static final String REQUESTED_TOKENS_HEADER = "X-RateLimit-Requested-Tokens";

	private final ReactiveRedisTemplate<String, String> template;
	private final RedisScript<Long> script = new DefaultRedisScript<>(getLuaScript(), Long.class);

	/**
	 * 해당 메서드는 제공된 키(ipAddress)를 기반으로 요청이 허용되는지 여부를 확인하고 RateResponse를 반환합니다.
	 * 1초당 최대 접근 가능한 횟수는 5회입니다.
	 */
	@Override
	public Mono<RateResponse> isAllowed(String ipAddress) {
		var keys = settingKey(ipAddress);
		var args = settingArgs();

		return template.execute(script, keys, args)
			.next()
			.map(count -> {
				var allowed = count <= Config.MAX_REQUESTS;
				var remainingCount = Config.MAX_REQUESTS - count;

				log.info("allowed:{}, remainingCount={}", allowed, remainingCount);
				return new RateResponse(allowed, count, getHeaders(remainingCount));
			})
			.defaultIfEmpty(RateResponse.access());
	}

	@Override
	public Mono<Boolean> increaseRequestCount(String ipAddress) {
		var key = getKey(ipAddress);

		return template.opsForValue()
			.increment(key)
			.flatMap(count -> template.expire(key, Duration.ofMinutes(1L)).thenReturn(count))
			.map(count -> count >= Config.MAX_REQUESTS);
	}

	private List<String> settingKey(String ipAddress) {
		return Collections.singletonList(ipAddress);
	}

	private List<String> settingArgs() {
		var currentTimeMillis = Instant.now().toEpochMilli();
		var windowSizeInMillis = Config.WINDOW_SIZE_IN_MILLIS;

		return Arrays.asList(
			"0.0",
			String.valueOf(currentTimeMillis - windowSizeInMillis),
			String.valueOf(currentTimeMillis),
			String.valueOf(currentTimeMillis),
			String.valueOf(windowSizeInMillis / 1000)
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

	private String getKey(String ipAddress) {
		return String.format("ip:string:%s", ipAddress);
	}

	private Map<String, String> getHeaders(long tokensLeft) {
		Map<String, String> headers = new HashMap<>();
		headers.put(REMAINING_HEADER, String.valueOf(tokensLeft));
		headers.put(REPLENISH_RATE_HEADER, "1");
		headers.put(BURST_CAPACITY_HEADER, "1");
		headers.put(REQUESTED_TOKENS_HEADER, "1");
		return headers;
	}


	static class Config {

		static final int MAX_REQUESTS = 5;
		static final long WINDOW_SIZE_IN_MILLIS = 1000;
	}
}
