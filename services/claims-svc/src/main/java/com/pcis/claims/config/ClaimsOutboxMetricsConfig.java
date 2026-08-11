package com.pcis.claims.config;

import com.pcis.observability.metrics.JdbcOutboxEventMetricsRepository;
import com.pcis.observability.metrics.OutboxEventMetricsRepository;
import com.pcis.observability.metrics.OutboxMetrics;
import com.pcis.outbox.OutboxProperties;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties(OutboxProperties.class)
@AutoConfigureAfter(MetricsAutoConfiguration.class)
public class ClaimsOutboxMetricsConfig {

  @Bean
  @ConditionalOnMissingBean(OutboxEventMetricsRepository.class)
  OutboxEventMetricsRepository claimsOutboxEventMetricsRepository(DataSource dataSource) {
    return new JdbcOutboxEventMetricsRepository(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(OutboxMetrics.class)
  OutboxMetrics claimsOutboxMetrics(
      MeterRegistry registry,
      OutboxEventMetricsRepository repository,
      OutboxProperties properties,
      Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "claims-svc");
    return new OutboxMetrics(registry, repository, serviceName, properties.getMetricsNamespace());
  }
}
