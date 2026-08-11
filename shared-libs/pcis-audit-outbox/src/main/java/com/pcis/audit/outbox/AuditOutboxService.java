package com.pcis.audit.outbox;

/**
 * Enlists a validated audit event in the caller's active database transaction.
 *
 * <p>Implementations require {@code Propagation.MANDATORY} — calling {@link #write(AuditEvent)}
 * outside an existing transaction must fail fast without persisting anything.
 */
public interface AuditOutboxService {

  void write(AuditEvent event);
}
