package com.pcis.billing.config;

import com.pcis.billing.batch.bil003b.config.BillingGenerationProperties;
import com.pcis.billing.batch.cmm001b.config.CommissionCalculationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  BillingConfigProperties.class,
  BillingGenerationProperties.class,
  CommissionCalculationProperties.class
})
public class BillingServiceConfig {}
