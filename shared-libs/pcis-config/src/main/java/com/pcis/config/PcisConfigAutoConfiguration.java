package com.pcis.config;

import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties(PcisTunableProperties.class)
public class PcisConfigAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  TunableRepository tunableRepository(JdbcTemplate jdbcTemplate) {
    return new TunableRepository(jdbcTemplate);
  }

  @Bean
  @ConditionalOnMissingBean
  TunableResolver tunableResolver(
      TunableRepository repository,
      PcisTunableProperties properties,
      MeterRegistry meterRegistry) {
    return new TunableResolver(repository, properties, meterRegistry);
  }
}
