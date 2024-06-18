package com.rate.limiter.infrastructure.ratelimiter.service;

import com.rate.limiter.infrastructure.ratelimiter.dto.RateResponse;

import reactor.core.publisher.Mono;

public interface RateLimiter {

	// Mono<RateResponse> isAllowed(String ipAddress);
	// Mono<Boolean> increaseRequestCount(String ipAddress);
	Mono<RateResponse> isAllowed(Long productId);
}
