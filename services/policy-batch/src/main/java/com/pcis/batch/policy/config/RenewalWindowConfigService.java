package com.pcis.batch.policy.config;

import com.pcis.config.TunableKey;
import com.pcis.config.TunableNotFoundException;
import com.pcis.config.TunableResolver;
import org.springframework.stereotype.Service;

@Service
public class RenewalWindowConfigService {

  private final TunableResolver tunableResolver;
  private final PolicyRenewalProperties properties;

  public RenewalWindowConfigService(
      TunableResolver tunableResolver, PolicyRenewalProperties properties) {
    this.tunableResolver = tunableResolver;
    this.properties = properties;
  }

  public int getRenewalWindowDays() {
    try {
      return tunableResolver.getInt(TunableKey.POLICY_RENEWAL_WINDOW_DAYS);
    } catch (TunableNotFoundException ex) {
      return properties.getRenewalWindowDays();
    }
  }
}
