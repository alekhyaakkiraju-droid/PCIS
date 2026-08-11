package com.pcis.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * JPA mapping for {@code outbox_events} from the V1 baseline schema.
 *
 * <p>{@link #isPublished()} reflects {@code STATUS = 'PUBLISHED'} for relay queries.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false, updatable = false)
  private Long id;

  @Column(name = "AGGREGATE_TYPE", nullable = false, length = 100)
  private String aggregateType;

  @Column(name = "AGGREGATE_ID", nullable = false, length = 100)
  private String aggregateId;

  @Column(name = "EVENT_TYPE", nullable = false, length = 100)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "PAYLOAD", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(name = "IDEMPOTENCY_KEY", nullable = false)
  private UUID idempotencyKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", nullable = false, length = 20)
  private OutboxEventStatus status = OutboxEventStatus.PENDING;

  @Column(name = "ATTEMPT_COUNT", nullable = false)
  private int attemptCount;

  @Column(name = "NEXT_ATTEMPT_AT")
  private Instant nextAttemptAt;

  @Column(name = "LAST_ERROR", length = 500)
  private String lastError;

  @Column(name = "CRT_USER", length = 10)
  private String createdBy;

  @Column(name = "CRT_TIMESTAMP")
  private Instant createdAt;

  @Column(name = "UPD_USER", length = 10)
  private String updatedBy;

  @Column(name = "UPD_TIMESTAMP")
  private Instant updatedAt;

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

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public boolean isPublished() {
    return status == OutboxEventStatus.PUBLISHED;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public void setAggregateType(String aggregateType) {
    this.aggregateType = aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(String aggregateId) {
    this.aggregateId = aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
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

  public OutboxEventStatus getStatus() {
    return status;
  }

  public void setStatus(OutboxEventStatus status) {
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

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
