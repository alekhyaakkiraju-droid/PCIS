package com.pcis.customer.outbox;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
  @Id @Column(name = "id", nullable = false, columnDefinition = "UUID") private UUID id;
  @Column(name = "aggregate_type", nullable = false, length = 50) private String aggregateType;
  @Column(name = "aggregate_id", nullable = false, length = 50) private String aggregateId;
  @Column(name = "event_type", nullable = false, length = 50) private String eventType;
  @Column(name = "payload", nullable = false, columnDefinition = "TEXT") private String payload;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "published", nullable = false) private boolean published = false;
  @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }
  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public String getAggregateType() { return aggregateType; }
  public void setAggregateType(String aggregateType) { this.aggregateType = aggregateType; }
  public String getAggregateId() { return aggregateId; }
  public void setAggregateId(String aggregateId) { this.aggregateId = aggregateId; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public boolean isPublished() { return published; }
  public void setPublished(boolean published) { this.published = published; }
}
