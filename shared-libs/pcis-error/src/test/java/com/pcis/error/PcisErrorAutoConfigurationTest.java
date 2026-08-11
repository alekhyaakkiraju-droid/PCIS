package com.pcis.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class PcisErrorAutoConfigurationTest {

  private final WebApplicationContextRunner contextRunner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(PcisErrorAutoConfiguration.class));

  @Test
  void registersPcisExceptionHandlerWhenMissing() {
    contextRunner.run(
        context -> assertThat(context).hasSingleBean(PcisExceptionHandler.class));
  }

  @Test
  void doesNotOverrideExistingPcisExceptionHandler() {
    contextRunner
        .withBean(PcisExceptionHandler.class, PcisExceptionHandler::new)
        .run(
            context ->
                assertThat(context.getBeansOfType(PcisExceptionHandler.class)).hasSize(1));
  }
}
