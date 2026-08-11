package com.pcis.customer.support;

import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Test JwtDecoder that accepts any token value and grants CUSTOMER_AGENT role. */
public final class TestJwtDecoder implements JwtDecoder {

  @Override
  public Jwt decode(String token) {
    return Jwt.withTokenValue(token)
        .header("alg", "none")
        .subject("test-customer-agent")
        .claim("roles", List.of("CUSTOMER_AGENT"))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
