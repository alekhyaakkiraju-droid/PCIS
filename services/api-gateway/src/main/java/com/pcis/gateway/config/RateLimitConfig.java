package com.pcis.gateway.config;

import java.util.Objects;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

  @Bean
  RedisRateLimiter redisRateLimiter() {
    return new RedisRateLimiter(100, 200);
  }

  @Bean
  KeyResolver rateLimitKeyResolver(RateLimitKeyResolver rateLimitKeyResolver) {
    return rateLimitKeyResolver::resolveKey;
  }

  @Bean
  RateLimitKeyResolver rateLimitKeyResolverBean() {
    return new RateLimitKeyResolver();
  }
}
