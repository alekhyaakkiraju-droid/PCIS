package com.pcis.claims.outbox;

import com.pcis.outbox.OutboxEvent;
import com.pcis.outbox.OutboxEventRepository;
import com.pcis.outbox.OutboxEventStatus;
import com.pcis.observability.MdcKeys;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimsOutboxWriter {

  private static final String AGGREGATE_TYPE = "Claim";

  private final OutboxEventRepository outboxEventRepository;

  public ClaimsOutboxWriter(OutboxEventRepository outboxEventRepository) {
    this.outboxEventRepository = outboxEventRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void writeDomainEvent(
      String aggregateId, String eventType, Map<String, Object> payload, UUID idempotencyKey) {
    Map<String, Object> enriched = new HashMap<>(payload);
    String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
    if (correlationId != null && !correlationId.isBlank()) {
      enriched.putIfAbsent("correlationId", correlationId);
    }
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType(AGGREGATE_TYPE);
    event.setAggregateId(aggregateId);
    event.setEventType(eventType);
    event.setPayload(enriched);
    event.setIdempotencyKey(idempotencyKey);
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedBy("CLMSVC");
    event.setCreatedAt(Instant.now());
    event.setNextAttemptAt(Instant.now());
    outboxEventRepository.save(event);
  }
}
