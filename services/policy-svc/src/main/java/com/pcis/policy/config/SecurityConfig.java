package com.pcis.policy.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Deny-by-default Spring Security 6 configuration for policy-svc.
 * /actuator/health and /actuator/readiness are permitted without authentication.
 * All other paths require a valid JWT issued by the configured Keycloak realm.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!local")
public class SecurityConfig {

  private static final URI UNAUTHENTICATED_TYPE =
      URI.create("https://pcis.example/problems/unauthenticated");
  private static final URI FORBIDDEN_TYPE =
      URI.create("https://pcis.example/problems/forbidden");

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers("/actuator/health", "/actuator/health/**",
                        "/actuator/readiness", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(authenticationEntryPoint(objectMapper)))
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                    .accessDeniedHandler(accessDeniedHandler(objectMapper)))
        .build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new RealmAccessRolesConverter());
    return converter;
  }

  static AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    return (HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) ->
        writeProblem(objectMapper, req, res, HttpStatus.UNAUTHORIZED, "Unauthenticated",
            UNAUTHENTICATED_TYPE);
  }

  static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) ->
        writeProblem(objectMapper, req, res, HttpStatus.FORBIDDEN, "Forbidden", FORBIDDEN_TYPE);
  }

  private static void writeProblem(
      ObjectMapper objectMapper,
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String title,
      URI type) throws IOException {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, title);
    problem.setType(type);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }

  /**
   * Extracts roles from Keycloak's {@code realm_access.roles} claim and maps them to
   * {@code ROLE_*} Spring Security authorities.
   * Returns empty authorities rather than throwing when the claim is absent or malformed.
   */
  static final class RealmAccessRolesConverter
      implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      Object realmAccess = jwt.getClaim("realm_access");
      if (!(realmAccess instanceof Map<?, ?> realmMap)) {
        return Collections.emptyList();
      }
      Object rolesObj = realmMap.get("roles");
      if (!(rolesObj instanceof List<?> rolesList)) {
        return Collections.emptyList();
      }
      List<GrantedAuthority> authorities = new ArrayList<>(rolesList.size());
      for (Object role : rolesList) {
        if (role == null) {
          continue;
        }
        String roleName = role.toString().trim();
        if (roleName.isBlank()) {
          continue;
        }
        String prefixed = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        authorities.add(new SimpleGrantedAuthority(prefixed));
      }
      return authorities;
    }
  }
}
