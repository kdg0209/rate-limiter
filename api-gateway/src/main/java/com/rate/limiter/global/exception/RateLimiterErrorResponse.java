package com.rate.limiter.global.exception;

public record RateLimiterErrorResponse(boolean allowed, long remainingCount, String message) {
}
