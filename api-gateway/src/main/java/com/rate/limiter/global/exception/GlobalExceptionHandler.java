package com.rate.limiter.global.exception;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

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
		ErrorResponse errorResponse = null;
		DataBuffer dataBuffer = null;
		DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

		Map<String, String> errors = new HashMap<>();
		if (throwable instanceof ResponseStatusException) {
			var exception = (ResponseStatusException)throwable;
			errorResponse = new ErrorResponseException(HttpStatus.TOO_MANY_REQUESTS);
			exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
			errors.put("message", exception.getMessage());
		} else {
			errorResponse = new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
			exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		var error = "Gateway Error";
		try {
			error = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errors);
		} catch (JsonProcessingException e) {
			bufferFactory.wrap("".getBytes());
		}

		byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
		var buffer = exchange.getResponse().bufferFactory().wrap(bytes);
		return exchange.getResponse().writeWith(Mono.just(buffer));
	}
}
