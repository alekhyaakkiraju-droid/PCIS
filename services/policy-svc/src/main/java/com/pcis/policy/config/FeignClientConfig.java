package com.pcis.policy.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {

  @Bean
  RequestInterceptor correlationIdPropagationInterceptor() {
    return requestTemplate -> {
      String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
      if (correlationId != null) {
        requestTemplate.header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
      }
    };
  }
}
