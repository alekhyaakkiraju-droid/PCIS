package com.pcis.authz.application;

import com.pcis.authz.contract.AuthorizationRequest;
import com.pcis.authz.contract.AuthorizationResponse;
import com.pcis.outbox.OutboxEvent;
import com.pcis.outbox.OutboxEventRepository;
import com.pcis.outbox.OutboxEventStatus;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Persists authorization decision audit events to the transactional outbox. */
@Component
public class AuthorizationDecisionOutboxWriter {

  public static final String AGGREGATE_TYPE = "AuthorizationDecision";
  public static final String EVENT_TYPE = "AuthorizationDecisionRecorded";

  private final OutboxEventRepository outboxEventRepository;

  public AuthorizationDecisionOutboxWriter(OutboxEventRepository outboxEventRepository) {
    this.outboxEventRepository = outboxEventRepository;
  }

  public void write(String principalId, AuthorizationRequest request, AuthorizationResponse response) {
    OutboxEvent event = new OutboxEvent();
    event.setAggregateType(AGGREGATE_TYPE);
    event.setAggregateId(response.correlationId());
    event.setEventType(EVENT_TYPE);
    event.setPayload(buildPayload(principalId, request, response));
    event.setIdempotencyKey(toIdempotencyKey(response.correlationId()));
    event.setStatus(OutboxEventStatus.PENDING);
    event.setAttemptCount(0);
    event.setCreatedBy(truncatePrincipal(principalId));
    event.setCreatedAt(Instant.now());
    event.setNextAttemptAt(Instant.now());
    outboxEventRepository.save(event);
  }

  private static Map<String, Object> buildPayload(
      String principalId, AuthorizationRequest request, AuthorizationResponse response) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("actor", principalId);
    payload.put("resource", request.resource());
    payload.put("operation", request.operation());
    payload.put("decision", response.decision().name());
    payload.put("reasonCode", response.reasonCode().name());
    payload.put("evaluatedPermissions", response.evaluatedPermissions());
    payload.put("correlationId", response.correlationId());
    if (!request.context().isEmpty()) {
      payload.put("context", request.context());
    }
    return payload;
  }

  private static String truncatePrincipal(String principalId) {
    if (principalId == null) {
      return "SYSTEM";
    }
    return principalId.length() <= 10 ? principalId : principalId.substring(0, 10);
  }

  static UUID toIdempotencyKey(String correlationId) {
    try {
      return UUID.fromString(correlationId);
    } catch (IllegalArgumentException ex) {
      return UUID.nameUUIDFromBytes(correlationId.getBytes(StandardCharsets.UTF_8));
    }
  }
}
