package com.pcis.audit.contract;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** Response returned after a successful audit event write. */
public record AuditEventResponse(
    @JsonProperty("audit_log_id") long auditLogId,
    @JsonProperty("correlation_id") UUID correlationId,
    @JsonProperty("operation") String operation,
    @JsonProperty("event_timestamp") Instant eventTimestamp) {}
