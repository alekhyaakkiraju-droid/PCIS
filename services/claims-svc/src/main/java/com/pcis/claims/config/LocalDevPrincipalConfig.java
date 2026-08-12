package com.pcis.claims.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@Profile("local")
public class LocalDevPrincipalConfig {

  private static final String ADJUSTER_ID = "ADJ90001";
  private static final String SUPERVISOR_ID = "SUP90001";

  @Bean
  @Order(0)
  OncePerRequestFilter localDevPrincipalFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
          String principal =
              request.getRequestURI().contains("/approvals") ? SUPERVISOR_ID : ADJUSTER_ID;
          List<SimpleGrantedAuthority> authorities =
              principal.equals(SUPERVISOR_ID)
                  ? List.of(
                      new SimpleGrantedAuthority("CLAIMS_SUPERVISOR"),
                      new SimpleGrantedAuthority("claims:read"),
                      new SimpleGrantedAuthority("claims:write"))
                  : List.of(
                      new SimpleGrantedAuthority("CLAIMS_ADJUSTER"),
                      new SimpleGrantedAuthority("claims:read"),
                      new SimpleGrantedAuthority("claims:write"));
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(principal, "local-dev", authorities));
        }
        filterChain.doFilter(request, response);
      }
    };
  }
}
