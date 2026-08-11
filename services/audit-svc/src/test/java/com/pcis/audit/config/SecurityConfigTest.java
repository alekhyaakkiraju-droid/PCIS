package com.pcis.audit.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {

  @Test
  void scopeAndRoleConverterHandlesCollectionScopeClaim() {
    var converter = new SecurityConfig.ScopeAndRoleConverter();
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("scope", java.util.List.of("audit:write"))
            .build();

    assertThat(converter.convert(jwt))
        .extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
        .containsExactly("audit:write");
  }

  @Test
  void scopeAndRoleConverterReturnsEmptyWhenNoScopeOrRoleClaims() {
    var converter = new SecurityConfig.ScopeAndRoleConverter();
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .build();

    assertThat(converter.convert(jwt)).isEmpty();
  }

  @Test
  void scopeAndRoleConverterMapsStringScopeAndRoles() {
    var converter = new SecurityConfig.ScopeAndRoleConverter();
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("test-user")
            .claim("scope", "audit:write other:scope")
            .claim("roles", java.util.List.of("audit-admin"))
            .build();

    assertThat(converter.convert(jwt))
        .extracting(org.springframework.security.core.GrantedAuthority::getAuthority)
        .contains("audit:write", "other:scope", "audit-admin");
  }
}
