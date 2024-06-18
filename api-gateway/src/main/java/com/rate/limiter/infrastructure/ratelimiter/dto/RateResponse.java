package com.rate.limiter.infrastructure.ratelimiter.dto;

import java.util.Map;

public record RateResponse(boolean allowed, long tokensRemaining, Map<String, String> headers) {

	public static RateResponse access() {
		return new RateResponse(true, 1L, null);
	}
}
