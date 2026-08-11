package com.pcis.gateway.config;

import com.pcis.gateway.security.PcisJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;

@Configuration
@Profile("!local")
public class GatewaySecurityBeansConfig {

  @Bean
  ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter(
      PcisJwtAuthenticationConverter converter) {
    return new ReactiveJwtAuthenticationConverterAdapter(converter);
  }
}
