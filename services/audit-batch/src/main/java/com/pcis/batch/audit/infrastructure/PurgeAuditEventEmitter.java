package com.pcis.batch.audit.infrastructure;

import com.pcis.batch.common.OutboxEventWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PurgeAuditEventEmitter {

  private final OutboxEventWriter outboxEventWriter;
  private final String actor;

  public PurgeAuditEventEmitter(
      OutboxEventWriter auditOutboxEventWriter,
      com.pcis.batch.audit.config.AuditPurgeProperties properties) {
    this.outboxEventWriter = auditOutboxEventWriter;
    this.actor = properties.getProgramName();
  }

  public void emit(String eventType, Map<String, Object> details) {
    Map<String, Object> payload = new LinkedHashMap<>(details);
    payload.put("eventType", eventType);
    payload.put("actor", actor);
    outboxEventWriter.write(
        "AUDIT_PURGE",
        String.valueOf(details.getOrDefault("target", "batch")),
        eventType,
        payload,
        UUID.randomUUID());
  }

  public void purgeStart() {
    emit("PURGE_START", Map.of("target", "audit-purge-job"));
  }

  public void partitionDropped(String partitionName) {
    emit("PURGE_PARTITION_DROPPED", Map.of("target", partitionName, "partition", partitionName));
  }

  public void keyScheduledDestroy(String keyArn, String s3Key) {
    emit(
        "PURGE_KEY_SCHEDULED_DESTROY",
        Map.of("target", s3Key, "kmsKeyArn", keyArn, "s3Key", s3Key));
  }

  public void purgeComplete(int partitionsPurged, int keysScheduled) {
    emit(
        "PURGE_COMPLETE",
        Map.of(
            "target",
            "audit-purge-job",
            "partitionsPurged",
            partitionsPurged,
            "keysScheduled",
            keysScheduled));
  }

  public void purgeFailed(String reason) {
    emit("PURGE_FAILED", Map.of("target", "audit-purge-job", "reason", reason));
  }
}
