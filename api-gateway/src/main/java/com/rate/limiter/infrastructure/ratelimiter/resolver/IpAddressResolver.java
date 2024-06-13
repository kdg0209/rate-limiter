package com.rate.limiter.infrastructure.ratelimiter.resolver;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class IpAddressResolver implements KeyResolver {

	@Override
	public Mono<String> resolve(ServerWebExchange exchange) {
		return Optional.of(exchange.getRequest().getRemoteAddress())
			.map(InetSocketAddress::getAddress)
			.map(InetAddress::getHostAddress)
			.map(Mono::just)
			.orElse(Mono.empty());
	}
}
