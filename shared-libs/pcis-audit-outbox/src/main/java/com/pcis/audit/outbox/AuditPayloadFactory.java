package com.pcis.audit.outbox;

import com.pcis.audit.contract.ValidatedAuditEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds v1 audit JSON payloads for {@code audit_outbox.PAYLOAD}. */
final class AuditPayloadFactory {

  private AuditPayloadFactory() {}

  static Map<String, Object> toPayload(ValidatedAuditEvent event, AuditPayloadMasker masker) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("action", event.actionCode().name());
    payload.put("old_value", masker.mask(event.oldValue()));
    payload.put("new_value", masker.mask(event.newValue()));
    payload.put("key", event.key());
    payload.put("service", event.service());
    payload.put("program", event.program());
    payload.put("actor", event.actor());
    payload.put("resource", event.resource());
    payload.put("field_name", event.fieldName());
    payload.put("correlation_id", event.correlationId().toString());
    payload.put("operation", event.operation().name());
    return payload;
  }
}
