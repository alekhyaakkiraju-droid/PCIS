package com.pcis.customer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for audit-svc event recording.
 * Circuit breaker fallback is fail-closed: throws AuditWriteException when audit-svc is
 * unavailable, preventing mutations from proceeding without an audit record.
 */
@FeignClient(
    name = "audit-svc",
    url = "${pcis.audit-svc.url}",
    fallback = AuditServiceClient.FailClosedFallback.class)
public interface AuditServiceClient {

  @PostMapping("/v1/audit/events")
  void recordEvent(@RequestBody AuditEventRequest event);

  record AuditEventRequest(
      String eventType,
      String subject,
      String resource,
      String action,
      String correlationId) {}

  /** Fail-closed fallback: circuit open → block mutation to preserve audit integrity. */
  @Component
  class FailClosedFallback implements AuditServiceClient {

    @Override
    public void recordEvent(AuditEventRequest event) {
      throw new AuditWriteException(
          "audit-svc unavailable — circuit open; mutation blocked to preserve audit integrity."
              + " correlationId=" + event.correlationId());
    }
  }
}
