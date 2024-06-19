package com.rate.limiter.infrastructure.ratelimiter.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import com.rate.limiter.infrastructure.ratelimiter.dto.RateResponse;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest
class SlidingWindowRateLimiterTest {

	@Autowired
	private SlidingWindowRateLimiter slidingWindowRateLimiter;

	@Autowired
	private ReactiveRedisTemplate<String, String> template;

	@Test
	@DisplayName("1초 동안 6번을 호출하면 5번까지는 허용하고 6번째 요청은 거절된다.")
	void 일정_기간_요청_테스트() {

		// given
		Long productId = 1L;

		// when
		Flux<RateResponse> rateResponseFlux = Flux.range(0, 6)
			.delayElements(Duration.ofMillis(1L))
			.flatMap(it -> slidingWindowRateLimiter.isAllowed(productId));

		// then
		StepVerifier.create(rateResponseFlux)
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isFalse())
			.verifyComplete();
	}

	@Test
	@DisplayName("1초에 한번씩 10번을 요청하면 모든 요청은 허용된다.")
	void _1초마다_10번을_요청하면_모든_요청은_허용될_수_있다() {

		// given
		Long productId = 2L;

		// when
		Flux<RateResponse> rateResponseFlux = Flux.range(0, 10)
			.delayElements(Duration.ofSeconds(1L))
			.flatMap(it -> slidingWindowRateLimiter.isAllowed(productId));

		// then
		StepVerifier.create(rateResponseFlux)
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.assertNext(it -> assertThat(it.allowed()).isTrue())
			.verifyComplete();
	}
}