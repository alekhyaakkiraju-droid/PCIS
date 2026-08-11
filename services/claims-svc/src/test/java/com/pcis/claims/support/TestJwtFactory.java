package com.pcis.claims.support;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Factory for creating test JWT objects. Uses Spring's in-memory Jwt builder —
 * no real signing, suitable for @WebMvcTest with @MockBean JwtDecoder.
 * For full integration tests with a real JwtDecoder, use TestJwtFactory with WireMock.
 */
public final class TestJwtFactory {

  private static final String TEST_SUBJECT = "test-claims-agent";

  private TestJwtFactory() {}

  public static Jwt claimsAdjuster() {
    return build(TEST_SUBJECT, "CLAIMS_ADJUSTER");
  }

  public static Jwt claimsSupervisor() {
    return build("test-supervisor", "CLAIMS_SUPERVISOR");
  }

  public static Jwt underwriter() {
    return build("test-underwriter", "UNDERWRITER");
  }

  public static Jwt readOnly() {
    return build("test-readonly", "CLAIMS_READ");
  }

  public static Jwt withRoles(String subject, String... roles) {
    return Jwt.withTokenValue("test-token-" + subject)
        .header("alg", "none")
        .subject(subject)
        .claim("realm_access", Map.of("roles", List.of(roles)))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  private static Jwt build(String subject, String role) {
    return withRoles(subject, role);
  }
}
