package com.pcis.audit.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultAuditOutboxService implements AuditOutboxService {

  private final OutboxEventRepository repository;
  private final AuditPayloadMasker masker;

  public DefaultAuditOutboxService(OutboxEventRepository repository, AuditPayloadMasker masker) {
    this.repository = repository;
    this.masker = masker;
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void write(AuditEvent event) {
    OutboxEvent outbox = new OutboxEvent();
    outbox.setPayload(AuditPayloadFactory.toPayload(event.validated(), masker));
    outbox.setIdempotencyKey(event.idempotencyKey());
    outbox.setStatus(OutboxStatus.PENDING);
    outbox.setAttemptCount(0);
    repository.save(outbox);
  }
}
