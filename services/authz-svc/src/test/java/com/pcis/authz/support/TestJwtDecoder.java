package com.pcis.authz.support;

import java.time.Instant;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Test JwtDecoder mapping token values to principal subjects for integration tests. */
public final class TestJwtDecoder implements JwtDecoder {

  @Override
  public Jwt decode(String token) {
    if ("invalid-token".equals(token) || "expired-token".equals(token)) {
      throw new BadJwtException("Token validation failed");
    }

    String subject =
        token.startsWith("principal:") ? token.substring("principal:".length()) : token;

    return Jwt.withTokenValue(token)
        .header("alg", "none")
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
