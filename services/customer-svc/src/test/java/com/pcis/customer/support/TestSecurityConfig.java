package com.pcis.customer.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
@Import(TestMaskingConfig.class)
public class TestSecurityConfig {

  @Bean
  JwtDecoder jwtDecoder() {
    return new TestJwtDecoder();
  }
}
