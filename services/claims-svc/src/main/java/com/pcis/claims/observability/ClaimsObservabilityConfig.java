package com.pcis.claims.observability;

import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnBean(ClaimsMetrics.class)
public class ClaimsObservabilityConfig {

  @Bean
  FilterRegistrationBean<ClaimsApiMetricsFilter> claimsApiMetricsFilterRegistration(
      ClaimsMetrics claimsMetrics) {
    FilterRegistrationBean<ClaimsApiMetricsFilter> registration =
        new FilterRegistrationBean<>(new ClaimsApiMetricsFilter(claimsMetrics));
    registration.setName("claimsApiMetricsFilter");
    registration.setDispatcherTypes(
        DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 200);
    registration.addUrlPatterns("/*");
    return registration;
  }
}
