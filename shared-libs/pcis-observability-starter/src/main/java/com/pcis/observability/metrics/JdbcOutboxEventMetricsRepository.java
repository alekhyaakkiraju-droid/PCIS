package com.pcis.observability.metrics;

import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JDBC implementation of {@link OutboxEventMetricsRepository} against {@code outbox_events}.
 *
 * <p>Pending events are rows with {@code STATUS = 'PENDING'}. Oldest-event age uses
 * {@code CRT_TIMESTAMP}.
 */
public class JdbcOutboxEventMetricsRepository implements OutboxEventMetricsRepository {

  private static final String COUNT_PENDING =
      """
      SELECT COUNT(*)
      FROM outbox_events
      WHERE STATUS = 'PENDING'
      """;

  private static final String OLDEST_PENDING =
      """
      SELECT MIN(CRT_TIMESTAMP)
      FROM outbox_events
      WHERE STATUS = 'PENDING'
      """;

  private final JdbcTemplate jdbcTemplate;

  public JdbcOutboxEventMetricsRepository(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  JdbcOutboxEventMetricsRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public long countPendingEvents() {
    Long count = jdbcTemplate.queryForObject(COUNT_PENDING, Long.class);
    return count != null ? count : 0L;
  }

  @Override
  public java.util.Optional<java.time.Instant> oldestPendingEventCreatedAt() {
    return java.util.Optional.ofNullable(
            jdbcTemplate.queryForObject(OLDEST_PENDING, java.sql.Timestamp.class))
        .map(java.sql.Timestamp::toInstant);
  }
}
