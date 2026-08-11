package com.pcis.audit.contract;

import java.util.UUID;

/** Immutable validated audit event ready for persistence. */
public record ValidatedAuditEvent(
    AuditActionCode actionCode,
    String oldValue,
    String newValue,
    String key,
    UUID correlationId,
    String service,
    String program,
    String actor,
    String resource,
    String fieldName,
    AuditOperation operation) {}
