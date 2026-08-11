package com.pcis.audit.support;

import java.time.Instant;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** Test JwtDecoder that accepts any token value and grants audit:write. */
public final class TestJwtDecoder implements JwtDecoder {

  @Override
  public Jwt decode(String token) {
    return Jwt.withTokenValue(token)
        .header("alg", "none")
        .subject("test-audit-writer")
        .claim("scope", "audit:write")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
