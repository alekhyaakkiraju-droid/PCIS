package com.pcis.batch.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class OutboxEventWriter {

  private static final String INSERT_SQL =
      """
      INSERT INTO outbox_events (
          AGGREGATE_TYPE,
          AGGREGATE_ID,
          EVENT_TYPE,
          PAYLOAD,
          IDEMPOTENCY_KEY,
          STATUS,
          ATTEMPT_COUNT,
          NEXT_ATTEMPT_AT,
          CRT_USER,
          CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?::jsonb, ?, 'PENDING', 0, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final String crtUser;

  public OutboxEventWriter(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, String crtUser) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.crtUser = crtUser;
  }

  public void write(
      String aggregateType,
      String aggregateId,
      String eventType,
      Map<String, Object> payload,
      UUID idempotencyKey) {
    String json;
    try {
      json = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("Unable to serialize outbox payload for " + eventType, ex);
    }
    Instant now = Instant.now();
    jdbcTemplate.update(
        INSERT_SQL,
        aggregateType,
        aggregateId,
        eventType,
        json,
        idempotencyKey,
        Timestamp.from(now),
        crtUser,
        Timestamp.from(now));
  }
}
