package com.pcis.premium.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import java.math.BigDecimal;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  @Bean
  Jackson2ObjectMapperBuilderCustomizer bigDecimalStringCustomizer() {
    return builder ->
        builder.modulesToInstall(
            new SimpleModule()
                .addSerializer(BigDecimal.class, new BigDecimalStringSerializer()));
  }
}
