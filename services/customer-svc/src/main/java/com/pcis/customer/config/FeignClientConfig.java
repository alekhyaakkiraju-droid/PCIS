package com.pcis.customer.config;

import com.pcis.observability.MdcKeys;
import com.pcis.observability.filter.CorrelationIdFilter;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.MDC;

/**
 * Feign client configuration for authz-svc and audit-svc.
 * Propagates X-Correlation-ID from MDC into outbound Feign requests for distributed tracing.
 * Circuit breaker configuration (fail-closed) is on the individual client fallback classes.
 */
@Configuration
public class FeignClientConfig {

  @Bean
  RequestInterceptor correlationIdPropagationInterceptor() {
    return requestTemplate -> {
      String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
      if (correlationId != null) {
        requestTemplate.header(CorrelationIdFilter.CORRELATION_HEADER, correlationId);
      }
    };
  }
}
