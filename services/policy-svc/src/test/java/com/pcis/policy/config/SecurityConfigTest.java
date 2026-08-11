package com.pcis.policy.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import com.pcis.policy.controller.PolicyController;
import com.pcis.policy.dto.PolicyListResponse;
import com.pcis.policy.dto.PolicyMapper;
import com.pcis.policy.service.PolicyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtDecoder jwtDecoder;

  @MockBean
  private PolicyService policyService;

  @MockBean
  private PolicyMapper policyMapper;

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
  void actuatorInfoPermittedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/actuator/info"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void apiPoliciesReturns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/policies"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void apiPoliciesPassesWithValidJwt() throws Exception {
    when(policyService.findPolicies(any(), any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));
    when(policyMapper.toListResponse(List.of(), 0, 20, 0))
        .thenReturn(new PolicyListResponse(List.of(), new PolicyListResponse.PageMetadata(0, 20, 0, 0)));

    mockMvc.perform(
            get("/api/v1/policies")
                .with(jwt().jwt(
                    Jwt.withTokenValue("test")
                        .header("alg", "none")
                        .subject("policy-agent")
                        .claim("realm_access", Map.of("roles", List.of("POLICY_AGENT")))
                        .build())))
        .andExpect(status().isOk());
  }

  @Test
  void realmAccessRolesConverterExtractsRolesWithPrefix() {
    var converter = new SecurityConfig.RealmAccessRolesConverter();
    Jwt jwt = Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("user")
        .claim("realm_access", Map.of("roles", List.of("POLICY_AGENT", "underwriter")))
        .build();

    var authorities = converter.convert(jwt);
    var names = authorities.stream()
        .map(a -> a.getAuthority())
        .toList();

    assertThat(names).contains("ROLE_POLICY_AGENT", "ROLE_underwriter");
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
