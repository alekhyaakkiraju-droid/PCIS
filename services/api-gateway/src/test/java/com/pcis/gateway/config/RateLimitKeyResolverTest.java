package com.pcis.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.test.StepVerifier;

class RateLimitKeyResolverTest {

  private RateLimitKeyResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new RateLimitKeyResolver();
  }

  @Test
  void resolvesKeyFromJwtSubject() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("principal-42")
            .build();
    var authentication = new UsernamePasswordAuthenticationToken(jwt, "n/a");

    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/customers").build());

    StepVerifier.create(
            resolver
                .resolveKey(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
        .expectNext("principal-42")
        .verifyComplete();
  }

  @Test
  void fallsBackToXForwardedForHeader() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/customers")
                .header("X-Forwarded-For", "203.0.113.10, 198.51.100.2")
                .build());

    StepVerifier.create(resolver.resolveKey(exchange)).expectNext("203.0.113.10").verifyComplete();
  }

  @Test
  void fallsBackToRemoteAddress() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/customers")
                .remoteAddress(new InetSocketAddress("192.168.1.5", 12345))
                .build());

    StepVerifier.create(resolver.resolveKey(exchange)).expectNext("192.168.1.5").verifyComplete();
  }

  @Test
  void returnsUnknownWhenNoPrincipalOrIp() {
    MockServerWebExchange exchange =
        MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/customers").build());

    assertThat(resolver.resolveClientIp(exchange)).isEqualTo("unknown");
    StepVerifier.create(resolver.resolveKey(exchange)).expectNext("unknown").verifyComplete();
  }

  @Test
  void ignoresBlankJwtSubject() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("   ").build();
    var authentication = new UsernamePasswordAuthenticationToken(jwt, "n/a");
    MockServerWebExchange exchange =
        MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/customers")
                .remoteAddress(new InetSocketAddress("10.0.0.1", 8080))
                .build());

    StepVerifier.create(
            resolver
                .resolveKey(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
        .expectNext("10.0.0.1")
        .verifyComplete();
  }
}
