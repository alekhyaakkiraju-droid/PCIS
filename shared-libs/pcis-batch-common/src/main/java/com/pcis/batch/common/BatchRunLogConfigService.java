package com.pcis.batch.common;

import com.pcis.config.TunableKey;
import com.pcis.config.TunableNotFoundException;
import com.pcis.config.TunableResolver;
import org.springframework.beans.factory.ObjectProvider;

/** Resolves batch run-log enablement from pcis-config tunables with property fallback. */
public class BatchRunLogConfigService {

  private final ObjectProvider<TunableResolver> tunableResolver;
  private final BatchRunLogProperties properties;

  public BatchRunLogConfigService(
      ObjectProvider<TunableResolver> tunableResolver, BatchRunLogProperties properties) {
    this.tunableResolver = tunableResolver;
    this.properties = properties;
  }

  public boolean isRunLogEnabled() {
    TunableResolver resolver = tunableResolver.getIfAvailable();
    if (resolver != null) {
      try {
        return resolver.getBoolean(TunableKey.BATCH_RUN_LOG_ENABLED);
      } catch (TunableNotFoundException ex) {
        // fall through to property default
      }
    }
    return properties.isEnabled();
  }
}
