package com.pcis.configsvc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

  private static final String PRINCIPAL_ID = "SM90001";

  @Bean
  @Order(0)
  OncePerRequestFilter localDevPrincipalFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
          List<SimpleGrantedAuthority> authorities =
              List.of(new SimpleGrantedAuthority("configuration-admin"));
          SecurityContextHolder.getContext()
              .setAuthentication(
                  new UsernamePasswordAuthenticationToken(
                      PRINCIPAL_ID, "local-dev", authorities));
        }
        filterChain.doFilter(request, response);
      }
    };
  }
}
