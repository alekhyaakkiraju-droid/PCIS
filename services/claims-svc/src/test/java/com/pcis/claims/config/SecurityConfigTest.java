package com.pcis.claims.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.claims.application.ClaimsApplicationService;
import com.pcis.claims.controller.ClaimsController;
import com.pcis.claims.dto.ClaimResponseMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ClaimsController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private JwtDecoder jwtDecoder;
  @MockBean private ClaimsApplicationService claimsApplicationService;
  @MockBean private ClaimResponseMapper claimResponseMapper;

  @Test
  void healthEndpointPermittedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void actuatorReadinessPermittedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/actuator/readiness"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void apiClaimsReturns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/claims")).andExpect(status().isUnauthorized());
  }

  @Test
  void apiClaimsPassesAuthWithValidJwt() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/claims")
                .with(
                    jwt()
                        .jwt(
                            Jwt.withTokenValue("test")
                                .header("alg", "none")
                                .subject("adjuster-01")
                                .claim("scope", List.of("claims:read"))
                                .build())
                        .authorities(new SimpleGrantedAuthority("claims:read"))))
        .andExpect(status().isOk());
  }

  @Test
  void unknownPathIsDeniedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/some/other/path")).andExpect(status().isUnauthorized());
  }

  @Test
  void unknownPathIsDeniedForAuthenticatedUser() throws Exception {
    mockMvc
        .perform(
            get("/some/other/path")
                .with(
                    jwt()
                        .jwt(
                            Jwt.withTokenValue("test")
                                .header("alg", "none")
                                .subject("adjuster-01")
                                .claim("scope", List.of("claims:read"))
                                .build())))
        .andExpect(status().isForbidden());
  }

  @Test
  void roleAndScopeConverterExtractsScopeAuthorities() {
    var converter = new SecurityConfig.RoleAndScopeConverter();
    Jwt jwt =
        Jwt.withTokenValue("t")
            .header("alg", "none")
            .subject("user")
            .claim("scope", List.of("claims:read", "claims:write"))
            .build();

    var names = converter.convert(jwt).stream().map(a -> a.getAuthority()).toList();
    assertThat(names).contains("claims:read", "claims:write");
  }

  @Test
  void roleAndScopeConverterReturnsEmptyWhenClaimAbsent() {
    var converter = new SecurityConfig.RoleAndScopeConverter();
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("user").build();
    assertThat(converter.convert(jwt)).isEmpty();
  }
}
