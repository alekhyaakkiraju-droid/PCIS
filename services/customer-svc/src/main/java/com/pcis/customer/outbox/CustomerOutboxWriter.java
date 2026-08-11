package com.pcis.customer.outbox;

import com.pcis.outbox.OutboxEvent;
import com.pcis.outbox.OutboxEventRepository;
import com.pcis.outbox.OutboxEventStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerOutboxWriter {

  private static final String AGGREGATE_TYPE = "Customer";

  private final OutboxEventRepository outboxEventRepository;

  public CustomerOutboxWriter(OutboxEventRepository outboxEventRepository) {
    this.outboxEventRepository = outboxEventRepository;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void writeDomainEvent(
      String aggregateId, String eventType, Map<String, Object> payload, UUID idempotencyKey) {
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType(AGGREGATE_TYPE);
    event.setAggregateId(aggregateId);
    event.setEventType(eventType);
    event.setPayload(payload);
    event.setIdempotencyKey(idempotencyKey);
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedBy("CUSTSVC");
    event.setCreatedAt(Instant.now());
    event.setNextAttemptAt(Instant.now());
    outboxEventRepository.save(event);
  }
}
