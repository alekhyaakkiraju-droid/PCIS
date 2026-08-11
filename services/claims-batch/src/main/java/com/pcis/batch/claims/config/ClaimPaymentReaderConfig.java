package com.pcis.batch.claims.config;

import com.pcis.claims.domain.ClaimReserveEntity;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClaimPaymentReaderConfig {

  private static final String PAYABLE_RESERVES_JPQL =
      """
      SELECT r FROM ClaimReserveEntity r
      JOIN FETCH r.claim c
      WHERE r.approvedAmt > r.paidToDate
      ORDER BY c.claimNbr, r.reserveId
      """;

  @Bean
  JpaPagingItemReader<ClaimReserveEntity> payableReserveReader(
      EntityManagerFactory entityManagerFactory, ClaimPaymentProperties properties) {
    JpaPagingItemReader<ClaimReserveEntity> reader = new JpaPagingItemReader<>();
    reader.setName("payableReserveReader");
    reader.setEntityManagerFactory(entityManagerFactory);
    reader.setQueryString(PAYABLE_RESERVES_JPQL);
    reader.setPageSize(properties.getChunkSize());
    Map<String, Object> params = new HashMap<>();
    reader.setParameterValues(params);
    return reader;
  }
}
