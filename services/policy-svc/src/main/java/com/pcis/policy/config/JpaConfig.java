package com.pcis.policy.config;

import java.util.Optional;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EntityScan(basePackages = {"com.pcis.policy.domain.entity", "com.pcis.outbox"})
@EnableJpaRepositories(basePackages = {"com.pcis.policy.domain.repository", "com.pcis.outbox"})
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
