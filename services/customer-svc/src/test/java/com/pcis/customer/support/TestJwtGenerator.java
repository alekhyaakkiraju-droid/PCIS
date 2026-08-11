package com.pcis.customer.support;

import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility for generating signed-in-memory JWTs for use in customer-svc tests.
 * All downstream story tests should use this generator rather than raw JWT construction
 * to keep claim structure consistent across the test suite.
 */
public final class TestJwtGenerator {

  private TestJwtGenerator() {}

  public static Jwt withScopes(String subject, String... scopes) {
    return Jwt.withTokenValue("test-token-" + subject)
        .header("alg", "none")
        .subject(subject)
        .claim("scope", String.join(" ", scopes))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  public static Jwt customerReader(String subject) {
    return withScopes(subject, "customer:read");
  }

  public static Jwt customerWriter(String subject) {
    return withScopes(subject, "customer:write");
  }

  public static Jwt duplicateOverride(String subject) {
    return withScopes(subject, "customer:write", "customer:duplicate-override");
  }

  public static Jwt withRoles(String subject, String... roles) {
    return Jwt.withTokenValue("test-token-" + subject)
        .header("alg", "none")
        .subject(subject)
        .claim("roles", List.of(roles))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  public static Jwt customerAgent(String subject) {
    return withRoles(subject, "CUSTOMER_AGENT");
  }

  public static Jwt underwriter(String subject) {
    return withRoles(subject, "UNDERWRITER");
  }

  public static Jwt supervisor(String subject) {
    return withRoles(subject, "SUPERVISOR");
  }

  public static Jwt readOnly(String subject) {
    return withRoles(subject, "READ_ONLY");
  }
}
