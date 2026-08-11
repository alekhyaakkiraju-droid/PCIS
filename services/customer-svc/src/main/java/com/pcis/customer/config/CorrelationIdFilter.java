package com.pcis.customer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propagates X-Correlation-ID into MDC for structured log correlation across services.
 * Generates a UUID when the header is absent. Clears MDC on response completion to
 * prevent thread-local leaks in pooled containers.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

  static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  static final String MDC_CORRELATION_ID = "correlationId";
  static final String MDC_SERVICE = "service";
  static final String MDC_ACTOR = "actor";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    try {
      MDC.put(MDC_CORRELATION_ID, correlationId);
      MDC.put(MDC_SERVICE, "customer-svc");
      String actor = resolveActor();
      if (actor != null) {
        MDC.put(MDC_ACTOR, actor);
      }
      response.setHeader(CORRELATION_ID_HEADER, correlationId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_CORRELATION_ID);
      MDC.remove(MDC_SERVICE);
      MDC.remove(MDC_ACTOR);
    }
  }

  private static String resolveActor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      return jwt.getSubject();
    }
    return null;
  }
}
