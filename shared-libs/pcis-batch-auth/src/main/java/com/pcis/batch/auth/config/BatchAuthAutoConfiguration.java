package com.pcis.batch.auth.config;

import com.pcis.batch.auth.BatchAuthRestTemplateInterceptor;
import com.pcis.batch.auth.BatchAuthenticationService;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import com.pcis.batch.auth.ClientSecretProvider;
import com.pcis.batch.auth.EnvironmentClientSecretProvider;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

/**
 * Auto-configuration for OAuth2 client-credentials batch authentication.
 */
@AutoConfiguration
@EnableConfigurationProperties(BatchAuthProperties.class)
@ConditionalOnProperty(prefix = "pcis.batch.oauth2", name = "token-uri")
public class BatchAuthAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ClientSecretProvider clientSecretProvider(Environment environment) {
    return new EnvironmentClientSecretProvider(environment);
  }

  @Bean
  @ConditionalOnMissingBean
  public BatchAuthenticationService batchAuthenticationService(
      BatchAuthProperties properties,
      ClientSecretProvider clientSecretProvider,
      RestTemplate batchAuthRestTemplate) {
    return new BatchAuthenticationService(properties, clientSecretProvider, batchAuthRestTemplate);
  }

  @Bean
  @ConditionalOnMissingBean(name = "batchAuthRestTemplate")
  public RestTemplate batchAuthRestTemplate() {
    return new RestTemplate();
  }

  @Bean
  @ConditionalOnClass(RestTemplate.class)
  @ConditionalOnMissingBean(BatchAuthRestTemplateInterceptor.class)
  public BatchAuthRestTemplateInterceptor batchAuthRestTemplateInterceptor(
      BatchAuthenticationService authenticationService) {
    return new BatchAuthRestTemplateInterceptor(authenticationService);
  }

  @Bean
  @ConditionalOnClass(RestTemplate.class)
  @ConditionalOnMissingBean(name = "batchAuthRestTemplateCustomizer")
  public RestTemplateCustomizer batchAuthRestTemplateCustomizer(
      BatchAuthRestTemplateInterceptor interceptor) {
    return restTemplate -> restTemplate.getInterceptors().add(interceptor);
  }

  @Bean
  @ConditionalOnClass(JobExecutionListener.class)
  @ConditionalOnMissingBean(BatchSecurityContextInitializer.class)
  public BatchSecurityContextInitializer batchSecurityContextInitializer(
      BatchAuthenticationService authenticationService) {
    return new BatchSecurityContextInitializer(authenticationService);
  }
}
