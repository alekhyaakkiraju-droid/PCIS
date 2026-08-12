package com.pcis.config;

import com.pcis.config.rules.RuleSetEvaluator;
import com.pcis.config.rules.RuleSetRepository;
import io.micrometer.core.instrument.MeterRegistry;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration(after = {DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class})
@ConditionalOnBean(DataSource.class)
@EnableConfigurationProperties({PcisTunableProperties.class, PcisCodeTableProperties.class})
public class PcisConfigAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  TunableRepository tunableRepository(JdbcTemplate jdbcTemplate) {
    return new TunableRepository(jdbcTemplate);
  }

  @Bean
  @ConditionalOnMissingBean
  CodeTableRepository codeTableRepository(JdbcTemplate jdbcTemplate) {
    return new CodeTableRepository(jdbcTemplate);
  }

  @Bean
  @ConditionalOnBean(JdbcTemplate.class)
  @ConditionalOnMissingBean
  TunableResolver tunableResolver(
      TunableRepository repository,
      PcisTunableProperties properties,
      MeterRegistry meterRegistry) {
    return new TunableResolver(repository, properties, meterRegistry);
  }

  @Bean
  @ConditionalOnBean(JdbcTemplate.class)
  @ConditionalOnMissingBean
  CodeTableService codeTableService(
      CodeTableRepository repository, PcisCodeTableProperties properties) {
    return new CodeTableService(repository, properties);
  }

  @Bean
  @ConditionalOnMissingBean
  RuleSetRepository ruleSetRepository(JdbcTemplate jdbcTemplate) {
    return new RuleSetRepository(jdbcTemplate);
  }

  @Bean
  @ConditionalOnBean(JdbcTemplate.class)
  @ConditionalOnMissingBean
  RuleSetEvaluator ruleSetEvaluator(
      RuleSetRepository repository, PcisCodeTableProperties properties) {
    return new RuleSetEvaluator(repository, properties);
  }
}
