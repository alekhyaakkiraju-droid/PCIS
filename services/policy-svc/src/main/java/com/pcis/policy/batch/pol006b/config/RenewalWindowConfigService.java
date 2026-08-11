package com.pcis.policy.batch.pol006b.config;

import com.pcis.config.TunableKey;
import com.pcis.config.TunableNotFoundException;
import com.pcis.config.TunableResolver;
import com.pcis.policy.config.PremiumProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RenewalWindowConfigService {

  private final TunableResolver tunableResolver;
  private final PolicyRenewalProperties properties;
  private final PremiumProperties premiumProperties;
  private final int premiumDefaultWindowDays;

  public RenewalWindowConfigService(
      TunableResolver tunableResolver,
      PolicyRenewalProperties properties,
      PremiumProperties premiumProperties,
      @Value("${pcis.premium.renewal-window-days:60}") int premiumDefaultWindowDays) {
    this.tunableResolver = tunableResolver;
    this.properties = properties;
    this.premiumProperties = premiumProperties;
    this.premiumDefaultWindowDays = premiumDefaultWindowDays;
  }

  public int getRenewalWindowDays() {
    if (properties.getRenewalWindowDays() != null) {
      return properties.getRenewalWindowDays();
    }
    try {
      return tunableResolver.getInt(TunableKey.POLICY_RENEWAL_WINDOW_DAYS);
    } catch (TunableNotFoundException ex) {
      return premiumProperties.getRenewalWindowDays() > 0
          ? premiumProperties.getRenewalWindowDays()
          : premiumDefaultWindowDays;
    }
  }
}
