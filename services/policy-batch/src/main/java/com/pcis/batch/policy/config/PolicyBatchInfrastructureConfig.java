package com.pcis.batch.policy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PolicyBatchInfrastructureConfig {

  @Bean
  RestTemplate premiumRatingRestTemplate() {
    return new RestTemplate();
  }
}
