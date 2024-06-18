package com.rate.limiter.global.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rate.limiter.infrastructure.ratelimiter.service.RateLimiter;
import com.rate.limiter.infrastructure.ratelimiter.service.RateLimiterFactory;

@Configuration
public class FilterConfig {

	private final RateLimiterFactory rateLimiterFactory;

	public FilterConfig(RateLimiter rateLimiter) {
		this.rateLimiterFactory = new RateLimiterFactory(rateLimiter);
	}

	@Bean
	public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
		var config = new RateLimiterFactory.Config();
		return builder.routes()
			.route(router -> router.path("/api/products/**")
				.filters(f -> f.filter(rateLimiterFactory.apply(config)))
				.uri("http://localhost:8081"))
			.build();
	}
}
