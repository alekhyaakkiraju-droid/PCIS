package com.pcis.classification;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@EnableConfigurationProperties(DataClassificationProperties.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "pcis.classification", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataClassificationAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  DataClassificationRegistry dataClassificationRegistry() {
    return new DataClassificationRegistry();
  }

  @Bean
  @ConditionalOnMissingBean
  DataClassificationLoader dataClassificationLoader(
      JdbcTemplate jdbcTemplate,
      DataClassificationProperties properties,
      DataClassificationRegistry registry,
      ResourceLoader resourceLoader) {
    return new DataClassificationLoader(jdbcTemplate, properties, registry, resourceLoader);
  }
}
