package com.pcis.audit.infrastructure.persistence;

import com.pcis.audit.infrastructure.persistence.entity.AuditLogEntity;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AuditLogJdbcWriter {

  private static final String INSERT_SQL =
      """
      INSERT INTO audit_log (
          action_cd, old_value, new_value, key_value, field_name,
          correlation_id, service_name, program_name, actor, resource_name,
          operation, event_timestamp, idempotency_key)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      RETURNING audit_log_id
      """;

  private final JdbcTemplate jdbcTemplate;

  public AuditLogJdbcWriter(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public AuditLogEntity insert(AuditLogEntity entity) {
    Instant eventTimestamp = entity.getEventTimestamp() != null ? entity.getEventTimestamp() : Instant.now();
    entity.setEventTimestamp(eventTimestamp);

    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var ps =
              connection.prepareStatement(
                  INSERT_SQL, new String[] {"audit_log_id"});
          ps.setString(1, entity.getActionCd());
          ps.setString(2, entity.getOldValue());
          ps.setString(3, entity.getNewValue());
          ps.setString(4, entity.getKeyValue());
          ps.setString(5, entity.getFieldName());
          ps.setObject(6, entity.getCorrelationId());
          ps.setString(7, entity.getServiceName());
          ps.setString(8, entity.getProgramName());
          ps.setString(9, entity.getActor());
          ps.setString(10, entity.getResourceName());
          ps.setString(11, entity.getOperation());
          ps.setTimestamp(12, Timestamp.from(eventTimestamp));
          ps.setObject(13, entity.getIdempotencyKey());
          return ps;
        },
        keyHolder);

    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("audit_log insert did not return audit_log_id");
    }
    entity.setAuditLogId(key.longValue());
    return entity;
  }
}
