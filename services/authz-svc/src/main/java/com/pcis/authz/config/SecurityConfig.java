package com.pcis.authz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final URI UNAUTHENTICATED_TYPE =
      URI.create("https://pcis.example/problems/unauthenticated");

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/actuator/readiness", "/actuator/readiness/**")
                    .permitAll()
                    .requestMatchers("/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/v1/authz/decisions")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(problemDetailEntryPoint(objectMapper)))
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(problemDetailEntryPoint(objectMapper)))
        .build();
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    return new JwtAuthenticationConverter();
  }

  private static AuthenticationEntryPoint problemDetailEntryPoint(ObjectMapper objectMapper) {
    return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
        -> writeProblemDetail(objectMapper, request, response, HttpStatus.UNAUTHORIZED, "Unauthorized");
  }

  static void writeProblemDetail(
      ObjectMapper objectMapper,
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String title)
      throws IOException {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, title);
    problem.setType(UNAUTHENTICATED_TYPE);
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}
