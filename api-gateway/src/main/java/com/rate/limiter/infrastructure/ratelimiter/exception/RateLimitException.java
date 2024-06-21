package com.rate.limiter.infrastructure.ratelimiter.exception;

import lombok.Getter;

@Getter
public class RateLimitException extends RuntimeException {

	private static final boolean allowed = false;
	private final long remainingCount;

	public RateLimitException(long remainingCount) {
		this.remainingCount = remainingCount;
	}
}
