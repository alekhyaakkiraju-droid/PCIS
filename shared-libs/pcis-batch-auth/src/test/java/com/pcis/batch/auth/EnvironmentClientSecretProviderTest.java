package com.pcis.batch.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class EnvironmentClientSecretProviderTest {

  @Test
  void resolvesDirectTestProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty(EnvironmentClientSecretProvider.DIRECT_SECRET_PROPERTY, "direct-secret");

    ClientSecretProvider provider = new EnvironmentClientSecretProvider(environment);

    assertThat(provider.resolve("arn:aws:secretsmanager:us-east-1:123:secret:test"))
        .isEqualTo("direct-secret");
  }

  @Test
  void resolvesFromConfiguredEnvironmentVariableName() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty(
        EnvironmentClientSecretProvider.SECRET_ENV_PROPERTY, "CUSTOM_BATCH_SECRET");
    environment.setProperty("CUSTOM_BATCH_SECRET", "env-secret");

    ClientSecretProvider provider = new EnvironmentClientSecretProvider(environment);

    assertThat(provider.resolve("arn:aws:secretsmanager:us-east-1:123:secret:test"))
        .isEqualTo("env-secret");
  }

  @Test
  void failsWhenSecretCannotBeResolved() {
    MockEnvironment environment = new MockEnvironment();
    ClientSecretProvider provider = new EnvironmentClientSecretProvider(environment);

    assertThatThrownBy(() -> provider.resolve("arn:aws:secretsmanager:us-east-1:123:secret:test"))
        .isInstanceOf(BatchConfigurationException.class)
        .hasMessageContaining("Unable to resolve client secret");
  }

  @Test
  void rejectsBlankSecretRef() {
    MockEnvironment environment = new MockEnvironment();
    ClientSecretProvider provider = new EnvironmentClientSecretProvider(environment);

    assertThatThrownBy(() -> provider.resolve(" "))
        .isInstanceOf(BatchConfigurationException.class);
  }
}
