package com.pcis.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.observability.metrics.JdbcOutboxEventMetricsRepository;
import com.pcis.observability.metrics.OutboxEventMetricsRepository;
import com.pcis.observability.metrics.OutboxMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@AutoConfiguration
@ConditionalOnClass({KafkaTemplate.class, jakarta.persistence.EntityManager.class})
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
public class OutboxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(DataSource.class)
  OutboxEventMetricsRepository outboxEventMetricsRepository(DataSource dataSource) {
    return new JdbcOutboxEventMetricsRepository(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean({OutboxEventMetricsRepository.class, MeterRegistry.class})
  OutboxMetrics outboxMetrics(
      MeterRegistry registry,
      OutboxEventMetricsRepository repository,
      OutboxProperties properties,
      org.springframework.core.env.Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "pcis-service");
    return new OutboxMetrics(registry, repository, serviceName, properties.getMetricsNamespace());
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(KafkaTemplate.class)
  KafkaOutboxEventPublisher kafkaOutboxEventPublisher(
      KafkaTemplate<?, ?> kafkaTemplate,
      ObjectMapper objectMapper,
      OutboxProperties properties) {
    return new KafkaOutboxEventPublisher(kafkaTemplate, objectMapper, properties);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(KafkaOutboxEventPublisher.class)
  @ConditionalOnProperty(prefix = "pcis.outbox", name = "relay-enabled", havingValue = "true", matchIfMissing = true)
  OutboxRelay outboxRelay(
      OutboxEventRepository repository,
      KafkaOutboxEventPublisher publisher,
      OutboxProperties properties,
      ObjectProvider<OutboxMetrics> outboxMetrics) {
    return new OutboxRelay(repository, publisher, properties, outboxMetrics);
  }
}
