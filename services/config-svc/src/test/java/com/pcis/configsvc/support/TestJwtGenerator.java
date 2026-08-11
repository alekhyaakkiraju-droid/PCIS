package com.pcis.configsvc.support;

import java.time.Instant;
import org.springframework.security.oauth2.jwt.Jwt;

public final class TestJwtGenerator {

  private TestJwtGenerator() {}

  public static Jwt configAdmin(String subject) {
    return Jwt.withTokenValue("test-token-" + subject)
        .header("alg", "none")
        .subject(subject)
        .claim("scope", "configuration-admin")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  public static Jwt unauthorizedReader(String subject) {
    return Jwt.withTokenValue("test-token-" + subject)
        .header("alg", "none")
        .subject(subject)
        .claim("scope", "customer:read")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
