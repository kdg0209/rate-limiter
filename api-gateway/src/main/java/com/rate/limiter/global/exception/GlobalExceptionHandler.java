package com.rate.limiter.global.exception;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rate.limiter.infrastructure.ratelimiter.exception.RateLimitException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Order(-2)
@Component
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

	private final ObjectMapper objectMapper;

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		return handleException(exchange, ex);
	}

	private Mono<Void> handleException(ServerWebExchange exchange, Throwable throwable) {
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

		var errorMessage = "";
		if (throwable instanceof RateLimitException) {
			var exception = (RateLimitException)throwable;
			errorMessage = handleRateLimiterException(exchange, exception);
		} else {
			exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		var bytes = errorMessage.getBytes(StandardCharsets.UTF_8);
		var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
		return exchange.getResponse().writeWith(Mono.just(buffer));
	}

	private String handleRateLimiterException(ServerWebExchange exchange, RateLimitException exception) {
		exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
		var result = "";

		try {
			var errorResponse = new RateLimiterErrorResponse(exception.isAllowed(), exception.getRemainingCount(), "");
			result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorResponse);
		} catch (JsonProcessingException e) {
			log.error("error message:{}", e.getMessage());
		}

		return result;
	}
}
