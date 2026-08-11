package com.pcis.claims.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {})
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtDecoder jwtDecoder;

  @Test
  void healthEndpointPermittedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  void actuatorReadinessPermittedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/readiness"))
        .andExpect(status().isOk());
  }

  @Test
  void apiClaimsReturns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/claims"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void apiClaimsPassesAuthWithValidJwt() throws Exception {
    mockMvc.perform(
            get("/api/v1/claims")
                .with(jwt().jwt(
                    Jwt.withTokenValue("test")
                        .header("alg", "none")
                        .subject("adjuster-01")
                        .claim("realm_access", Map.of("roles", List.of("CLAIMS_ADJUSTER")))
                        .build())))
        .andExpect(status().isNotFound()); // 404 means auth passed — no controller mapped yet
  }

  @Test
  void unknownPathIsDeniedWithoutAuthentication() throws Exception {
    // denyAll rule — unauthenticated users see 401 (authenticationEntryPoint fires first)
    mockMvc.perform(get("/some/other/path"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownPathIsDeniedForAuthenticatedUser() throws Exception {
    // denyAll rule — authenticated users see 403
    mockMvc.perform(
            get("/some/other/path")
                .with(jwt().jwt(
                    Jwt.withTokenValue("test")
                        .header("alg", "none")
                        .subject("adjuster-01")
                        .claim("realm_access", Map.of("roles", List.of("CLAIMS_ADJUSTER")))
                        .build())))
        .andExpect(status().isForbidden());
  }

  @Test
  void realmAccessRolesConverterExtractsRolesWithPrefix() {
    var converter = new SecurityConfig.RealmAccessRolesConverter();
    Jwt jwt = Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("user")
        .claim("realm_access", Map.of("roles", List.of("CLAIMS_ADJUSTER", "CLAIMS_READ")))
        .build();

    var authorities = converter.convert(jwt);
    var names = authorities.stream().map(a -> a.getAuthority()).toList();

    assertThat(names).contains("ROLE_CLAIMS_ADJUSTER", "ROLE_CLAIMS_READ");
  }

  @Test
  void realmAccessRolesConverterReturnsEmptyWhenClaimAbsent() {
    var converter = new SecurityConfig.RealmAccessRolesConverter();
    Jwt jwt = Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("user")
        .build();

    assertThat(converter.convert(jwt)).isEmpty();
  }

  @Test
  void realmAccessRolesConverterHandlesMissingRolesKey() {
    var converter = new SecurityConfig.RealmAccessRolesConverter();
    Jwt jwt = Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("user")
        .claim("realm_access", Map.of("other", "value"))
        .build();

    assertThat(converter.convert(jwt)).isEmpty();
  }
}
