package com.pcis.premium.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.common.OutboxEventWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OutboxConfig {

  @Bean
  @org.springframework.context.annotation.Primary
  OutboxEventWriter ratingOutboxEventWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, "PRMCLC01");
  }
}
