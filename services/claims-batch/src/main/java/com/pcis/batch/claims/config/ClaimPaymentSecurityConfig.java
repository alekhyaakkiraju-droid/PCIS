package com.pcis.batch.claims.config;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class ClaimPaymentSecurityConfig {

  @Bean
  @ConditionalOnMissingBean(name = "batchSecurityContextInitializer")
  JobExecutionListener claimPaymentPrincipalListener(ClaimPaymentProperties properties) {
    return new JobExecutionListener() {
      @Override
      public void beforeJob(JobExecution jobExecution) {
        Jwt jwt =
            Jwt.withTokenValue("batch-local-token")
                .header("alg", "none")
                .subject(properties.getBatchServicePrincipal())
                .build();
        SecurityContextHolder.getContext()
            .setAuthentication(new JwtAuthenticationToken(jwt));
      }

      @Override
      public void afterJob(JobExecution jobExecution) {
        SecurityContextHolder.clearContext();
      }
    };
  }
}
