package com.pcis.claims.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class ClaimsApiMetricsFilter extends OncePerRequestFilter {

  private final ClaimsMetrics claimsMetrics;

  public ClaimsApiMetricsFilter(ClaimsMetrics claimsMetrics) {
    this.claimsMetrics = claimsMetrics;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path != null && path.startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    io.micrometer.core.instrument.Timer.Sample sample = claimsMetrics.startApiRequest();
    try {
      filterChain.doFilter(request, response);
    } finally {
      claimsMetrics.recordApiRequest(
          sample,
          request.getMethod(),
          normalizeUri(request.getRequestURI()),
          response.getStatus());
    }
  }

  static String normalizeUri(String uri) {
    if (uri == null) {
      return "unknown";
    }
    return uri.replaceAll("/CLM\\d+", "/{claimNbr}").replaceAll("/\\d+", "/{id}");
  }
}
