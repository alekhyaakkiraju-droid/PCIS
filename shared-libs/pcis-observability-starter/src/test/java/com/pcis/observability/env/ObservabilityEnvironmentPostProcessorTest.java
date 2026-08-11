package com.pcis.observability.env;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ObservabilityEnvironmentPostProcessorTest {

  @Test
  void registersActuatorHealthGroupsAndTraceSampleDefaults() {
    ObservabilityEnvironmentPostProcessor processor = new ObservabilityEnvironmentPostProcessor();
    MockEnvironment environment = new MockEnvironment();
    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("management.endpoint.health.probes.enabled"))
        .isEqualTo("true");
    assertThat(environment.getProperty("management.endpoint.health.group.liveness.include"))
        .contains("livenessState");
    assertThat(environment.getProperty("management.endpoint.health.group.readiness.include"))
        .contains("db")
        .contains("keycloak");
    assertThat(environment.getProperty("management.endpoint.health.group.startup.include"))
        .contains("flyway");
    assertThat(environment.getProperty("pcis.observability.trace-sample-rate")).isEqualTo("0.1");
    assertThat(environment.getProperty("otel.traces.sampler")).isEqualTo("parentbased_traceidratio");
    assertThat(environment.getProperty("otel.traces.sampler.arg")).isEqualTo("0.1");
    assertThat(environment.getPropertySources().contains(ObservabilityEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
        .isTrue();
  }

  @Test
  void applicationOverridesWinOverDefaults() {
    ObservabilityEnvironmentPostProcessor processor = new ObservabilityEnvironmentPostProcessor();
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("pcis.observability.trace-sample-rate", "0.5");
    environment.setProperty("management.endpoints.web.exposure.include", "health");
    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("pcis.observability.trace-sample-rate")).isEqualTo("0.5");
    assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
  }
}
