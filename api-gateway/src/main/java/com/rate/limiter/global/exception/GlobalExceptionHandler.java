package com.rate.limiter.global.exception;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

		BaseErrorResponse baseErrorResponse = null;
		if (throwable instanceof ResponseStatusException) {
			var exception = (ResponseStatusException)throwable;
			exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			baseErrorResponse = new BaseErrorResponse(exception.getMessage());
		} else {
			exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		var error = "";
		try {
			error = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(baseErrorResponse);
		} catch (JsonProcessingException e) {
			log.error("error message:{}", e.getMessage());
		}

		var bytes = error.getBytes(StandardCharsets.UTF_8);
		var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
		return exchange.getResponse().writeWith(Mono.just(buffer));
	}
}
