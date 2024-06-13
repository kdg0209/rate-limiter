package com.rate.limiter.infrastructure.ratelimiter.service;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import lombok.Getter;
import reactor.core.publisher.Mono;

public class RateLimiterFactory extends AbstractGatewayFilterFactory<RateLimiterFactory.Config> {

	private final RateLimiter rateLimiter;

	public RateLimiterFactory(RateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (ServerWebExchange exchange, GatewayFilterChain chain) -> {
			String ipAddress = extractIpAddress(exchange);

			return rateLimiter.isAllowed(ipAddress)
				.flatMap(isAllowed -> {
					if (!isAllowed) {
						return rateLimiter.increaseRequestCount(ipAddress)
							.flatMap(overRequestCount -> {
								if (isOverMaxCount(overRequestCount)) {
									//
								}
								return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS));
							});
					}
					return chain.filter(exchange);
				});
		};
	}

	private String extractIpAddress(ServerWebExchange exchange) {
		return exchange.getRequest()
			.getRemoteAddress().getAddress()
			.getHostAddress();
	}

	private boolean isOverMaxCount(Boolean isTrue) {
		return Boolean.TRUE.equals(isTrue);
	}

	@Getter
	public static class Config {

		int statusCode;
	}
}
