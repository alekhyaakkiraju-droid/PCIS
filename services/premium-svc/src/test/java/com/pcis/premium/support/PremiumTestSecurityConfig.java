package com.pcis.premium.support;

import java.time.Instant;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class PremiumTestSecurityConfig {

  @Bean
  JwtDecoder jwtDecoder() {
    return token -> {
      String subject = token.contains("reader") ? "premium-reader" : "premium-rater";
      List<String> roles =
          token.contains("reader") ? List.of("premium:read") : List.of("premium:rate");
      return Jwt.withTokenValue(token)
          .header("alg", "none")
          .subject(subject)
          .claim("realm_access", java.util.Map.of("roles", roles))
          .issuedAt(Instant.now())
          .expiresAt(Instant.now().plusSeconds(3600))
          .build();
    };
  }
}
