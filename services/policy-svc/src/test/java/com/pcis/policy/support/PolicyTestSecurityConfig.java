package com.pcis.policy.support;

import java.time.Instant;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class PolicyTestSecurityConfig {

  @Bean
  JwtDecoder jwtDecoder() {
    return token ->
        Jwt.withTokenValue(token)
            .header("alg", "none")
            .subject("test-policy-agent")
            .claim("realm_access", java.util.Map.of("roles", List.of("POLICY_AGENT")))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();
  }
}
