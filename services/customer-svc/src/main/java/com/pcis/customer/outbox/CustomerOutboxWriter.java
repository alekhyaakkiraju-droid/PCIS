package com.pcis.customer.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerOutboxWriter {

  private static final String AGGREGATE_TYPE = "Customer";

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  public CustomerOutboxWriter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
    this.outboxEventRepository = outboxEventRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void writeAuditEvent(String aggregateId, String eventType, Map<String, Object> payload) {
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType(AGGREGATE_TYPE);
    event.setAggregateId(aggregateId);
    event.setEventType(eventType);
    event.setPayload(toJson(payload));
    outboxEventRepository.save(event);
  }

  private String toJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize outbox payload", ex);
    }
  }
}
