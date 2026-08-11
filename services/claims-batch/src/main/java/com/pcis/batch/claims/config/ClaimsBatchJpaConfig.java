package com.pcis.batch.claims.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "batchAuditorProvider")
public class ClaimsBatchJpaConfig {

  @Bean
  AuditorAware<String> batchAuditorProvider() {
    return () -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
        return Optional.of(auth.getName());
      }
      return Optional.of("BATCH_SVC");
    };
  }
}
