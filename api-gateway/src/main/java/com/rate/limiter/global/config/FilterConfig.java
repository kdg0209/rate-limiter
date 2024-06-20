package com.rate.limiter.global.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rate.limiter.infrastructure.ratelimiter.service.RateLimiter;
import com.rate.limiter.infrastructure.ratelimiter.service.RateLimiterFilterFactory;

@Configuration
public class FilterConfig {

	private final RateLimiterFilterFactory rateLimiterFilterFactory;

	public FilterConfig(RateLimiter rateLimiter) {
		this.rateLimiterFilterFactory = new RateLimiterFilterFactory(rateLimiter);
	}

	@Bean
	public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
		var config = new RateLimiterFilterFactory.Config();
		return builder.routes()
			.route(router -> router.path("/api/products/**")
				.filters(f -> f.filter(rateLimiterFilterFactory.apply(config)))
				.uri("http://localhost:8081"))
			.build();
	}
}
