package com.pcis.audit.outbox;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@AutoConfiguration
@ConditionalOnClass({EntityManager.class, AuditOutboxService.class})
@EntityScan(basePackageClasses = OutboxEvent.class)
@EnableJpaRepositories(basePackageClasses = OutboxEventRepository.class)
public class AuditOutboxAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  AuditPayloadMasker auditPayloadMasker() {
    return new SimpleJsonMaskingStub();
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(OutboxEventRepository.class)
  AuditOutboxService auditOutboxService(
      OutboxEventRepository repository, AuditPayloadMasker masker) {
    return new DefaultAuditOutboxService(repository, masker);
  }
}
