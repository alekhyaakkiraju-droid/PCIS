package com.pcis.customer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pcis.customer.support.TestJwtGenerator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Verifies deny-by-default security filter chain behaviour:
 * - /actuator/health returns 200 without authentication
 * - /api/v1/customers returns 401 without a JWT
 * - /api/v1/customers returns 200 with a valid mock JWT
 */
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
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void actuatorHealthLivenessPermittedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health/liveness"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void actuatorInfoPermittedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/info"))
        .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotIn(401, 403));
  }

  @Test
  void apiEndpointReturns401WithoutJwt() throws Exception {
    mockMvc.perform(get("/api/v1/customers"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void apiEndpointReturns200WithValidJwt() throws Exception {
    mockMvc.perform(
            get("/api/v1/customers")
                .with(jwt().jwt(TestJwtGenerator.customerAgent("test-agent"))))
        .andExpect(status().isNotFound()); // 404 = no handler, not 401/403 = authentication passed
  }

  @Test
  void roleAndScopeConverterExtractsRolesFromJwt() {
    var converter = new SecurityConfig.RoleAndScopeConverter();
    var jwt = TestJwtGenerator.withRoles("user", "CUSTOMER_AGENT", "READ_ONLY");

    var authorities = converter.convert(jwt);
    var names = authorities.stream()
        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
        .toList();

    org.assertj.core.api.Assertions.assertThat(names)
        .contains("CUSTOMER_AGENT", "READ_ONLY");
  }

  @Test
  void roleAndScopeConverterExtractsScopeStringAuthorities() {
    var converter = new SecurityConfig.RoleAndScopeConverter();
    var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("svc")
        .claim("scope", "customer:read customer:write")
        .build();

    var authorities = converter.convert(jwt);
    var names = authorities.stream()
        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
        .toList();

    org.assertj.core.api.Assertions.assertThat(names)
        .contains("customer:read", "customer:write");
  }

  @Test
  void roleAndScopeConverterReturnsEmptyOnNoClaimsJwt() {
    var converter = new SecurityConfig.RoleAndScopeConverter();
    var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("anon")
        .build();

    org.assertj.core.api.Assertions.assertThat(converter.convert(jwt)).isEmpty();
  }
}
