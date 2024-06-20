package com.rate.limiter.infrastructure.ratelimiter.exception;

import lombok.Getter;

@Getter
public class RateLimitException extends RuntimeException {

	private final boolean allowed;
	private final long remainingCount;

	public RateLimitException(boolean allowed, long remainingCount) {
		this.allowed = allowed;
		this.remainingCount = remainingCount;
	}
}
