package com.pcis.masking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.classification.DataClassificationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PiiMaskingAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(DataClassificationRegistry.class, MaskingTestSupport::registry)
          .withConfiguration(
              AutoConfigurations.of(JacksonAutoConfiguration.class, PiiMaskingAutoConfiguration.class));

  @Test
  void autoConfiguresMaskingServiceAndJacksonModule() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(MaskingService.class);
          assertThat(context).hasSingleBean(com.pcis.masking.logback.LogbackMaskingInitializer.class);
          ObjectMapper objectMapper = context.getBean(ObjectMapper.class);
          assertThat(objectMapper.getRegisteredModuleIds())
              .anyMatch(id -> id.toString().contains("PcisJacksonMaskingModule"));
        });
  }

  @Test
  void canBeDisabledViaProperty() {
    contextRunner
        .withPropertyValues("pcis.pii-masking.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(MaskingService.class));
  }
}
