package com.rate.limiter.infrastructure.ratelimiter.service;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.rate.limiter.infrastructure.ratelimiter.exception.RateLimitException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterFilterFactory extends AbstractGatewayFilterFactory<RateLimiterFilterFactory.Config> {

	private final RateLimiter rateLimiter;

	@Override
	public GatewayFilter apply(Config config) {
		return (ServerWebExchange exchange, GatewayFilterChain chain) -> {
			var productId = extractProductId(exchange);
			var ipAddress = extractIpAddress(exchange);

			return rateLimiter.isAllowed(productId)
				.flatMap(response -> {
					log.info("response={}", response);
					if (!response.allowed()) {
						var rateLimitException = new RateLimitException(response.remainingCount());
						return Mono.error(rateLimitException);
					}
					return chain.filter(exchange);
				});
		};
	}

	private Long extractProductId(ServerWebExchange exchange) {
		return Long.valueOf(exchange.getRequest().getQueryParams().getFirst("productId"));
	}

	private String extractIpAddress(ServerWebExchange exchange) {
		return exchange.getRequest()
			.getRemoteAddress().getAddress()
			.getHostAddress();
	}

	private boolean isOverMaxCount(Boolean isTrue) {
		return Boolean.TRUE.equals(isTrue);
	}


	public static class Config {

	}
}
