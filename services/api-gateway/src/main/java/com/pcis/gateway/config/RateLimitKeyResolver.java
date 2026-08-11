package com.pcis.gateway.config;

import java.util.Objects;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Resolves rate-limit keys from JWT subject or client IP. */
public class RateLimitKeyResolver {

  public Mono<String> resolveKey(ServerWebExchange exchange) {
    return ReactiveSecurityContextHolder.getContext()
        .map(securityContext -> securityContext.getAuthentication())
        .filter(Objects::nonNull)
        .filter(authentication -> authentication.getPrincipal() instanceof Jwt)
        .map(authentication -> ((Jwt) authentication.getPrincipal()).getSubject())
        .filter(subject -> subject != null && !subject.isBlank())
        .switchIfEmpty(Mono.fromSupplier(() -> resolveClientIp(exchange)));
  }

  public String resolveClientIp(ServerWebExchange exchange) {
    String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    if (exchange.getRequest().getRemoteAddress() != null
        && exchange.getRequest().getRemoteAddress().getAddress() != null) {
      return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
    }
    return "unknown";
  }
}
