package com.pcis.policy.config;

import java.util.Optional;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import com.pcis.policy.config.PremiumProperties;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableConfigurationProperties(PremiumProperties.class)
@EntityScan(
    basePackages = {
      "com.pcis.policy.domain.entity",
      "com.pcis.policy.batch.pol006b.domain.entity",
      "com.pcis.outbox"
    })
@EnableJpaRepositories(
    basePackages = {
      "com.pcis.policy.domain.repository",
      "com.pcis.policy.batch.pol006b.domain.repository",
      "com.pcis.outbox"
    })
public class JpaConfig {

  @Bean
  AuditorAware<String> auditorProvider() {
    return () -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
        return Optional.of(jwt.getSubject());
      }
      return Optional.of("system");
    };
  }
}
