package com.pcis.claims.observability;

import com.pcis.observability.MdcKeys;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Propagates {@code X-Correlation-ID} from MDC into OpenTelemetry span attributes and baggage
 * (WO-200). Sampling is configured via {@code pcis.observability.trace-sample-rate} and OTel
 * auto-instrumentation from pcis-observability-starter.
 */
@Configuration
public class TracingConfig {

  @Bean
  FilterRegistrationBean<CorrelationTracePropagationFilter> correlationTracePropagationFilter() {
    FilterRegistrationBean<CorrelationTracePropagationFilter> registration =
        new FilterRegistrationBean<>(new CorrelationTracePropagationFilter());
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    return registration;
  }

  static final class CorrelationTracePropagationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
      if (StringUtils.hasText(correlationId)) {
        Span.current().setAttribute("correlation.id", correlationId);
        Baggage.current()
            .toBuilder()
            .put("correlation.id", correlationId)
            .build()
            .makeCurrent();
      }
      filterChain.doFilter(request, response);
    }
  }
}
