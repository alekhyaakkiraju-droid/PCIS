package com.pcis.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.gateway.support.JwtTestSupport;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class PcisJwtAuthenticationConverterTest {

  private PcisJwtAuthenticationConverter converter;

  @BeforeEach
  void setUp() {
    converter = new PcisJwtAuthenticationConverter();
  }

  @Test
  void mapsRealmRolesToSpringAuthorities() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user-1")
            .claim("realm_access", Map.of("roles", List.of("claims-adjuster", "ROLE_SUPERVISOR")))
            .claim("authority_limit", 25000)
            .build();

    var authentication = converter.convert(jwt);

    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_claims-adjuster", "ROLE_SUPERVISOR");
    assertThat(authentication.getDetails()).isEqualTo(Map.of("authority_limit", 25000));
  }

  @Test
  void returnsEmptyAuthoritiesWhenRealmAccessMissing() {
    Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("user-1").build();

    Collection<GrantedAuthority> authorities = converter.extractAuthorities(jwt);

    assertThat(authorities).isEmpty();
  }

  @Test
  void ignoresBlankRoles() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user-1")
            .claim("realm_access", Map.of("roles", List.of("", "  ", "agent")))
            .build();

    assertThat(converter.extractAuthorities(jwt))
        .containsExactly(new SimpleGrantedAuthority("ROLE_agent"));
  }

  @Test
  void extractRolesHandlesMalformedRealmAccess() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("user-1")
            .claim("realm_access", "not-a-map")
            .build();

    assertThat(converter.extractRoles(jwt)).isEmpty();
  }

  @Test
  void convertsSignedFixtureToken() {
    Jwt jwt =
        Jwt.withTokenValue(JwtTestSupport.validToken())
            .header("alg", "RS256")
            .subject(JwtTestSupport.SUBJECT)
            .claim("realm_access", Map.of("roles", List.of("claims-adjuster")))
            .build();

    assertThat(converter.extractAuthorities(jwt))
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_claims-adjuster");
  }
}
