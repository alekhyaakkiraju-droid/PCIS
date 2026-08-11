package com.pcis.claims.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestJwtFactory {

  private static final String TEST_SUBJECT = "adjuster01";

  private TestJwtFactory() {}

  public static Jwt claimsReader() {
    return withScopes(TEST_SUBJECT, "claims:read");
  }

  public static Jwt claimsWriter() {
    return withScopes(TEST_SUBJECT, "claims:read", "claims:write");
  }

  public static RequestPostProcessor asClaimsReader() {
    return jwt().jwt(claimsReader()).authorities(new SimpleGrantedAuthority("claims:read"));
  }

  public static RequestPostProcessor asClaimsWriter() {
    return jwt()
        .jwt(claimsWriter())
        .authorities(
            new SimpleGrantedAuthority("claims:read"),
            new SimpleGrantedAuthority("claims:write"));
  }

  public static Jwt withScopes(String subject, String... scopes) {
    return Jwt.withTokenValue("test-token-" + subject)
        .header("alg", "none")
        .subject(subject)
        .claim("scope", List.of(scopes))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }

  public static RequestPostProcessor authenticateWithScopes(String subject, String... scopes) {
    return jwt()
        .jwt(withScopesJwt(subject, scopes))
        .authorities(
            Arrays.stream(scopes).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
  }

  private static Jwt withScopesJwt(String subject, String... scopes) {
    return Jwt.withTokenValue("test-token-" + subject)
        .header("alg", "none")
        .subject(subject)
        .claim("scope", List.of(scopes))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build();
  }
}
