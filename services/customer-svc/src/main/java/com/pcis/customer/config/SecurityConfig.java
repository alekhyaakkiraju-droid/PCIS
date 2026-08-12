package com.pcis.customer.config;

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
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(authenticationEntryPoint(objectMapper)))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
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
        writeProblemDetail(objectMapper, req, res, HttpStatus.UNAUTHORIZED, "Unauthenticated",
            UNAUTHENTICATED_TYPE);
  }

  static AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) ->
        writeProblemDetail(objectMapper, req, res, HttpStatus.FORBIDDEN, "Forbidden",
            FORBIDDEN_TYPE);
  }

  private static void writeProblemDetail(
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

  /** Extracts scope, roles claim, and maps wireframe roles to customer domain permissions. */
  static final class RoleAndScopeConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final java.util.Map<String, List<String>> ROLE_PERMISSIONS =
        java.util.Map.of(
            "CSR", List.of("customer:read", "customer:write", "customer:duplicate-override"),
            "CLAIMS_SUPERVISOR",
                List.of("customer:read", "customer:duplicate-override"),
            "COMPLIANCE", List.of("customer:read"),
            "UNDERWRITER", List.of("customer:read"),
            "CLAIMS_ADJUSTER", List.of("customer:read"));

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
      java.util.LinkedHashSet<String> authorityNames = new java.util.LinkedHashSet<>();

      Object scopeClaim = jwt.getClaim("scope");
      if (scopeClaim instanceof String s) {
        java.util.Arrays.stream(s.split("\\s+"))
            .filter(v -> v != null && !v.isBlank())
            .forEach(authorityNames::add);
      } else if (scopeClaim instanceof Collection<?> c) {
        c.stream().map(Object::toString).filter(v -> !v.isBlank()).forEach(authorityNames::add);
      }

      Object rolesClaim = jwt.getClaim("roles");
      if (rolesClaim instanceof Collection<?> c) {
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

      if (authorityNames.isEmpty()) {
        return Collections.emptyList();
      }

      return authorityNames.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
  }
}
