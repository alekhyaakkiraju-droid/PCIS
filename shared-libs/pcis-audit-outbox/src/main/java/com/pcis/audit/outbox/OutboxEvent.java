package com.pcis.audit.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** JPA mapping for {@code audit_outbox} — audit-specific transactional outbox rows. */
@Entity
@Table(name = "audit_outbox")
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false, updatable = false)
  private Long id;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "PAYLOAD", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "IDEMPOTENCY_KEY", nullable = false)
  private UUID idempotencyKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  private OutboxStatus status = OutboxStatus.PENDING;

  @Column(name = "ATTEMPT_COUNT", nullable = false)
  private int attemptCount;

  @Column(name = "NEXT_ATTEMPT_AT")
  private Instant nextAttemptAt;

  @Column(name = "CREATED_AT", nullable = false)
  private Instant createdAt;

  @Column(name = "LAST_ERROR", length = 500)
  private String lastError;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (nextAttemptAt == null) {
      nextAttemptAt = now;
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }

  public UUID getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(UUID idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxStatus status) {
    this.status = status;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(int attemptCount) {
    this.attemptCount = attemptCount;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void setNextAttemptAt(Instant nextAttemptAt) {
    this.nextAttemptAt = nextAttemptAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }
}
