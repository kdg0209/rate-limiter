package com.rate.limiter.infrastructure.ratelimiter.service;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import reactor.core.publisher.Mono;

@SpringBootTest
class SlidingWindowRateLimiterTest {

	@Autowired
	ReactiveRedisTemplate<String, String> template;

	@Test
	void 아이피_주소를_통해_lua_스크립트를_한번_실행하면_TRUE의_값을_반환받을_수_있다() {

		// given
		String ipAddress = "222.108.94.197";
		SlidingWindowRateLimiter slidingWindowRateLimiter = new SlidingWindowRateLimiter(template);

		// when
		Mono<Boolean> result = slidingWindowRateLimiter.isAllowed(ipAddress);

		// then
		assertThat(result.block()).isTrue();
	}

	@Test
	void 아이피_주소를_통해_lua_스크립트를_세번_실행하면_FALSE의_값을_반환받을_수_있다() throws InterruptedException {

		// given
		String ipAddress = "222.108.94.197";
		SlidingWindowRateLimiter slidingWindowRateLimiter = new SlidingWindowRateLimiter(template);

		// when
		int threadCount = 4;
		CountDownLatch latch = new CountDownLatch(threadCount);
		List<CompletableFuture<Mono<Boolean>>> futures = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			var future = CompletableFuture.supplyAsync(() -> {
				try {
					return slidingWindowRateLimiter.isAllowed(ipAddress);
				} finally {
					latch.countDown();
				}
			});
			futures.add(future);
		}
		latch.await();

		// then
		List<Mono<Boolean>> result = futures.stream()
			.map(CompletableFuture::join)
			.collect(Collectors.toList());

		result.stream()
				.map(Mono::block)
				.forEach(System.out::println);
	}

	@Test
	void 아이피_주소를_통해_Lua_스크립트의_인자를_반환받을_수_있다() {

		// given
		SlidingWindowRateLimiter slidingWindowRateLimiter = new SlidingWindowRateLimiter(template);

		// when
		List<String> test = slidingWindowRateLimiter.createKeys("222.108.94.197");
		List<String> result = slidingWindowRateLimiter.createArgs();

		System.out.println(test);
		System.out.println(result);
		// then
		// assertThat(result)

	}
}