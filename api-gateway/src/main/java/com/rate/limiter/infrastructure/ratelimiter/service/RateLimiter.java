package com.rate.limiter.infrastructure.ratelimiter.service;

import reactor.core.publisher.Mono;

public interface RateLimiter {

	Mono<Boolean> isAllowed(String ipAddress);
	Mono<Boolean> increaseRequestCount(String ipAddress);
}
