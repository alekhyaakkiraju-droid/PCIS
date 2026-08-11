package com.pcis.audit.outbox;

import com.pcis.audit.contract.AuditEventRequest;
import com.pcis.audit.contract.AuditEventValidator;
import com.pcis.audit.contract.ValidatedAuditEvent;
import java.util.UUID;

/** Audit event to enlist in the caller's active transaction via {@link AuditOutboxService}. */
public record AuditEvent(ValidatedAuditEvent validated, UUID idempotencyKey) {

  public static AuditEvent of(AuditEventRequest request, UUID idempotencyKey) {
    return new AuditEvent(AuditEventValidator.validate(request), idempotencyKey);
  }
}
