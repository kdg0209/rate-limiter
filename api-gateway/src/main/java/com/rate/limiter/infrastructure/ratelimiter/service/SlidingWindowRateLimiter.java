package com.rate.limiter.infrastructure.ratelimiter.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public final class SlidingWindowRateLimiter implements RateLimiter {

	private static final String SLIDING_WINDOW_LUA_SCRIPT = "sliding-window.lua";

	private final ReactiveRedisTemplate<String, String> template;
	private final DefaultRedisScript<Long> script = new DefaultRedisScript<>(getLuaScript(), Long.class);

	@Override
	public Mono<Boolean> isAllowed(String ipAddress) {
		var keys = createKeys(ipAddress);
		var args = createArgs();

		return template.execute(script, keys, args)
			.next()
			.map(count -> count <= Config.MAX_REQUESTS)
			.defaultIfEmpty(true);
	}

	@Override
	public Mono<Boolean> increaseRequestCount(String ipAddress) {
		var key = getKey(ipAddress);

		return template.opsForValue()
			.increment(key)
			.flatMap(count -> template.expire(key, Duration.ofMinutes(1L)).thenReturn(count))
			.map(count -> count >= 3);
	}

	List<String> createKeys(String ipAddress) {
		return Collections.singletonList(ipAddress);
	}

	List<String> createArgs() {
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

	static class Config {

		static final int MAX_REQUESTS = 4;
		static final long WINDOW_SIZE_IN_MILLIS = 1000;
	}
}
