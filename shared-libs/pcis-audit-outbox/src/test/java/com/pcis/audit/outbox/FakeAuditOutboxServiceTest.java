package com.pcis.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.audit.contract.AuditActionCode;
import com.pcis.audit.contract.AuditOperation;
import com.pcis.audit.contract.ValidatedAuditEvent;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FakeAuditOutboxServiceTest {

  private final FakeAuditOutboxService service = new FakeAuditOutboxService();

  @BeforeEach
  void reset() {
    service.clear();
  }

  @Test
  void recordsWrittenEvents() {
    AuditEvent event = sampleEvent(UUID.randomUUID());

    service.write(event);

    assertThat(service.getWrittenEvents()).containsExactly(event);
  }

  @Test
  void canSimulateWriteFailure() {
    service.setFailOnNextWrite(true);

    assertThatThrownBy(() -> service.write(sampleEvent(UUID.randomUUID())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("simulated audit outbox write failure");
    assertThat(service.getWrittenEvents()).isEmpty();
  }

  private static AuditEvent sampleEvent(UUID idempotencyKey) {
    ValidatedAuditEvent validated =
        new ValidatedAuditEvent(
            AuditActionCode.UPD,
            "old",
            "new",
            "KEY-1",
            UUID.randomUUID(),
            "billing-svc",
            "BIL003B",
            "BATCHUSER",
            "Installment",
            "STATUS",
            AuditOperation.UPDATE);
    return new AuditEvent(validated, idempotencyKey);
  }
}
