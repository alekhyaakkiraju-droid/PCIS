package com.pcis.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class PcisErrorAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  PcisExceptionHandler pcisExceptionHandler() {
    return new PcisExceptionHandler();
  }
}
