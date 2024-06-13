package com.rate.limiter.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.rate.limiter.global.properties.RedisProperties;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@RequiredArgsConstructor
public class RedisConfig {

	private final RedisProperties redisProperties;

	@Bean
	@Primary
	public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
		return new LettuceConnectionFactory(redisProperties.host(), redisProperties.port());
	}

	@Bean
	public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate() {
		var serializationContext = RedisSerializationContext.<String, Object>newSerializationContext(new StringRedisSerializer())
				.hashKey(new StringRedisSerializer())
				.hashValue(new GenericJackson2JsonRedisSerializer())
				.build();

		return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory(), serializationContext);
	}
}
