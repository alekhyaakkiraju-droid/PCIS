package com.pcis.audit.infrastructure.persistence.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class AuditLogEntityId implements Serializable {

  private Long auditLogId;
  private Instant eventTimestamp;

  public AuditLogEntityId() {}

  public AuditLogEntityId(Long auditLogId, Instant eventTimestamp) {
    this.auditLogId = auditLogId;
    this.eventTimestamp = eventTimestamp;
  }

  public Long getAuditLogId() {
    return auditLogId;
  }

  public void setAuditLogId(Long auditLogId) {
    this.auditLogId = auditLogId;
  }

  public Instant getEventTimestamp() {
    return eventTimestamp;
  }

  public void setEventTimestamp(Instant eventTimestamp) {
    this.eventTimestamp = eventTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AuditLogEntityId that)) {
      return false;
    }
    return Objects.equals(auditLogId, that.auditLogId)
        && Objects.equals(eventTimestamp, that.eventTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(auditLogId, eventTimestamp);
  }
}
