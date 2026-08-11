package com.pcis.batch.auth;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Resolves client secrets from environment variables or test-only Spring properties.
 *
 * <p>Production yaml supplies {@code client-secret-ref} (ARN placeholder). The secret value is
 * injected via environment variable {@code PCIS_BATCH_OAUTH2_CLIENT_SECRET} or an override from
 * {@code pcis.batch.oauth2.client-secret-env}. For local/integration tests only, {@code
 * pcis.batch.oauth2.client-secret} may be set directly.
 */
public class EnvironmentClientSecretProvider implements ClientSecretProvider {

  static final String DIRECT_SECRET_PROPERTY = "pcis.batch.oauth2.client-secret";
  static final String SECRET_ENV_PROPERTY = "pcis.batch.oauth2.client-secret-env";
  static final String DEFAULT_SECRET_ENV = "PCIS_BATCH_OAUTH2_CLIENT_SECRET";

  private final Environment environment;

  public EnvironmentClientSecretProvider(Environment environment) {
    this.environment = environment;
  }

  @Override
  public String resolve(String secretRef) {
    if (!StringUtils.hasText(secretRef)) {
      throw new BatchConfigurationException("pcis.batch.oauth2.client-secret-ref must be set");
    }

    String direct = environment.getProperty(DIRECT_SECRET_PROPERTY);
    if (StringUtils.hasText(direct)) {
      return direct;
    }

    String envKey =
        firstNonBlank(
            environment.getProperty(SECRET_ENV_PROPERTY), DEFAULT_SECRET_ENV);
    String value = environment.getProperty(envKey);
    if (!StringUtils.hasText(value)) {
      value = System.getenv(envKey);
    }
    if (!StringUtils.hasText(value)) {
      throw new BatchConfigurationException(
          "Unable to resolve client secret for ref "
              + secretRef
              + "; set "
              + envKey
              + " or "
              + DIRECT_SECRET_PROPERTY
              + " for tests");
    }
    return value;
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }
}
