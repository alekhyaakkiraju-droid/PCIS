package com.pcis.batch.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.auth.BatchAuthenticationService;
import com.pcis.batch.auth.BatchAuthRestTemplateInterceptor;
import com.pcis.batch.auth.BatchSecurityContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateCustomizer;

class BatchAuthAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(BatchAuthAutoConfiguration.class))
          .withPropertyValues(
              "pcis.batch.oauth2.token-uri=http://localhost:8089/token",
              "pcis.batch.oauth2.client-id=batch-audit",
              "pcis.batch.oauth2.client-secret-ref=arn:aws:secretsmanager:us-east-1:123456789012:secret:batch-audit",
              "pcis.batch.oauth2.client-secret=test-secret",
              "pcis.batch.oauth2.scope=batch:audit");

  @Test
  void registersBeansWhenTokenUriConfigured() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(BatchAuthenticationService.class);
          assertThat(context).hasSingleBean(BatchAuthRestTemplateInterceptor.class);
          assertThat(context).hasSingleBean(BatchSecurityContextInitializer.class);
          assertThat(context).hasSingleBean(RestTemplateCustomizer.class);
          assertThat(context.getBean(BatchAuthProperties.class).getExpirationBufferSeconds())
              .isEqualTo(30);
        });
  }

  @Test
  void disabledWithoutTokenUri() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(BatchAuthAutoConfiguration.class))
        .run(context -> assertThat(context).doesNotHaveBean(BatchAuthenticationService.class));
  }
}
