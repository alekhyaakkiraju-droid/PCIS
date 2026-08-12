package com.pcis.claims.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

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
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/actuator/readiness",
                        "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/api/v1/claims/**")
                    .authenticated()
                    .requestMatchers("/api/v1/customers/*/claims/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
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
    converter.setJwtGrantedAuthoritiesConverter(new RoleAndScopeConverter());
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

  /** Extracts scope, roles, realm_access.roles, and maps wireframe roles to claims permissions. */
  static final class RoleAndScopeConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final java.util.Map<String, List<String>> ROLE_PERMISSIONS =
        java.util.Map.of(
            "CLAIMS_ADJUSTER", List.of("claims:read", "claims:write"),
            "CLAIMS_SUPERVISOR", List.of("claims:read", "claims:write"),
            "CSR", List.of("claims:read"),
            "COMPLIANCE", List.of("claims:read"));

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      java.util.LinkedHashSet<String> authorityNames = new java.util.LinkedHashSet<>();
      addScopeAuthorities(jwt, authorityNames);
      addRoleAuthorities(jwt.getClaim("roles"), authorityNames);
      Object realmAccess = jwt.getClaim("realm_access");
      if (realmAccess instanceof java.util.Map<?, ?> realmMap) {
        addRoleAuthorities(realmMap.get("roles"), authorityNames);
      }
      if (authorityNames.isEmpty()) {
        return Collections.emptyList();
      }
      return authorityNames.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    private static void addScopeAuthorities(Jwt jwt, java.util.Set<String> authorityNames) {
      Object scopeClaim = jwt.getClaim("scope");
      if (scopeClaim instanceof String s) {
        java.util.Arrays.stream(s.split("\\s+"))
            .filter(v -> v != null && !v.isBlank())
            .forEach(authorityNames::add);
      } else if (scopeClaim instanceof Collection<?> c) {
        c.stream().map(Object::toString).filter(v -> !v.isBlank()).forEach(authorityNames::add);
      }
    }

    private static void addRoleAuthorities(Object rolesClaim, java.util.Set<String> authorityNames) {
      if (!(rolesClaim instanceof Collection<?> c)) {
        return;
      }
      c.stream()
          .map(Object::toString)
          .filter(v -> v != null && !v.isBlank())
          .forEach(
              role -> {
                authorityNames.add(role);
                String normalized = role.startsWith("ROLE_") ? role.substring(5) : role;
                List<String> mapped = ROLE_PERMISSIONS.get(normalized);
                if (mapped != null) {
                  authorityNames.addAll(mapped);
                }
              });
    }
  }
}
