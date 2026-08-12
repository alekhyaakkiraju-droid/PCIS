package com.pcis.claims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PolicyClientConfig {

  @Bean
  RestClient policyRestClient(@Value("${pcis.policy-svc.url:http://127.0.0.1:8084}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
  }
}
