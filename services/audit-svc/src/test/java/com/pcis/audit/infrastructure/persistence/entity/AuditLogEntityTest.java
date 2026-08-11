package com.pcis.audit.infrastructure.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditLogEntityTest {

  @Test
  void compositeIdEqualsAndHashCode() {
    var id1 = new AuditLogEntityId(1L, Instant.parse("2026-08-11T00:00:00Z"));
    var id2 = new AuditLogEntityId(1L, Instant.parse("2026-08-11T00:00:00Z"));
    var id3 = new AuditLogEntityId(2L, Instant.parse("2026-08-11T00:00:00Z"));

    assertThat(id1).isEqualTo(id2).hasSameHashCodeAs(id2).isNotEqualTo(id3);
  }

  @Test
  void entityAccessorsRoundTrip() {
    AuditLogEntity entity = new AuditLogEntity();
    entity.setAuditLogId(99L);
    entity.setEventTimestamp(Instant.parse("2026-08-11T12:00:00Z"));
    entity.setActionCd("PAY");
    entity.setActor("batchclm");
    entity.setOldValue("old");
    entity.setNewValue("new");
    entity.setKeyValue("key");
    entity.setFieldName("field");
    entity.setCorrelationId(java.util.UUID.randomUUID());
    entity.setServiceName("claims-svc");
    entity.setProgramName("CLM006B");
    entity.setResourceName("CLAIM_T");
    entity.setOperation("PAY");

    assertThat(entity.getAuditLogId()).isEqualTo(99L);
    assertThat(entity.getActionCd()).isEqualTo("PAY");
    assertThat(entity.getActor()).isEqualTo("batchclm");
    assertThat(entity.getServiceName()).isEqualTo("claims-svc");
    assertThat(entity.getOperation()).isEqualTo("PAY");
  }

  @Test
  void compositeIdDefaultConstructorAndSetters() {
    var id = new AuditLogEntityId();
    id.setAuditLogId(1L);
    id.setEventTimestamp(Instant.parse("2026-08-11T00:00:00Z"));
    assertThat(id.getAuditLogId()).isEqualTo(1L);
    assertThat(id.getEventTimestamp()).isNotNull();
  }
}
