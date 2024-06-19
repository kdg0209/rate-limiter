package com.rate.limiter.infrastructure.ratelimiter.dto;

import java.util.Map;

/**
 *
 * @param allowed 허용 가능 여부
 * @param remainingCount 남은 접근 횟수
 * @param headers 헤더 파라미터
 */
public record RateResponse(boolean allowed, long remainingCount, Map<String, String> headers) {

	public static RateResponse access() {
		return new RateResponse(true, 1L, null);
	}
}
