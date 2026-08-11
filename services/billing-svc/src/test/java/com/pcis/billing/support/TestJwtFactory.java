package com.pcis.billing.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestJwtFactory {

  private static final String TEST_SUBJECT = "billing-user";

  private TestJwtFactory() {}

  public static Jwt billingReader() {
    return withScopes(TEST_SUBJECT, "billing:read");
  }

  public static Jwt billingWriter() {
    return withScopes(TEST_SUBJECT, "billing:read", "billing:write");
  }

  public static RequestPostProcessor asBillingReader() {
    return jwt().jwt(billingReader()).authorities(new SimpleGrantedAuthority("billing:read"));
  }

  public static RequestPostProcessor asBillingWriter() {
    return jwt()
        .jwt(billingWriter())
        .authorities(
            new SimpleGrantedAuthority("billing:read"),
            new SimpleGrantedAuthority("billing:write"));
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
        .jwt(withScopes(subject, scopes))
        .authorities(
            Arrays.stream(scopes).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new));
  }
}
