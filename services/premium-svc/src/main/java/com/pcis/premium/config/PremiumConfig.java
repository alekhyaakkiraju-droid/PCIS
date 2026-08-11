package com.pcis.premium.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PremiumRatingProperties.class)
public class PremiumConfig {}
