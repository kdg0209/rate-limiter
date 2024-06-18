package com.rate.limiter.infrastructure.ratelimiter.dto;

import java.util.Map;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class RateResponse {

	private final boolean allowed;
	private final long tokensRemaining;
	private final Map<String, String> headers;

	public RateResponse(boolean allowed, Map<String, String> headers) {
		this.allowed = allowed;
		this.tokensRemaining = 1;
		this.headers = headers;
	}

	public RateResponse(boolean allowed, long tokensRemaining, Map<String, String> headers) {
		this.allowed = allowed;
		this.tokensRemaining = tokensRemaining;
		this.headers = headers;
	}

	public static RateResponse access() {
		return new RateResponse(true, null);
	}
}
