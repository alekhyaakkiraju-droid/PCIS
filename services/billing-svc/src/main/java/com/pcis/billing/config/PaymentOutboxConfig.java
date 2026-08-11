package com.pcis.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.batch.common.OutboxEventWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PaymentOutboxConfig {

  @Bean
  OutboxEventWriter paymentOutboxEventWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    return new OutboxEventWriter(jdbcTemplate, objectMapper, "BILPAY");
  }
}
