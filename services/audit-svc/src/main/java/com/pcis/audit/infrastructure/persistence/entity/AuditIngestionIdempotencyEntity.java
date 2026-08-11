package com.pcis.audit.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_ingestion_idempotency")
public class AuditIngestionIdempotencyEntity {

  @Id
  @Column(name = "idempotency_key", nullable = false)
  private UUID idempotencyKey;

  @Column(name = "audit_log_id", nullable = false)
  private Long auditLogId;

  @Column(name = "event_timestamp", nullable = false)
  private Instant eventTimestamp;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;

  protected AuditIngestionIdempotencyEntity() {}

  public AuditIngestionIdempotencyEntity(
      UUID idempotencyKey, Long auditLogId, Instant eventTimestamp, Instant ingestedAt) {
    this.idempotencyKey = idempotencyKey;
    this.auditLogId = auditLogId;
    this.eventTimestamp = eventTimestamp;
    this.ingestedAt = ingestedAt;
  }

  public UUID getIdempotencyKey() {
    return idempotencyKey;
  }

  public Long getAuditLogId() {
    return auditLogId;
  }

  public Instant getEventTimestamp() {
    return eventTimestamp;
  }
}
