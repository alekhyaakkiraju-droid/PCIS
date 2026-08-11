package com.pcis.audit.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pcis.audit.contract.AuditEventRequest;
import com.pcis.audit.contract.AuditOperation;
import com.pcis.audit.infrastructure.masking.MaskingService;
import com.pcis.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.pcis.audit.infrastructure.persistence.AuditLogJdbcWriter;
import com.pcis.audit.infrastructure.persistence.repository.AuditIngestionIdempotencyRepository;
import com.pcis.audit.infrastructure.persistence.repository.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

  @Mock private AuditLogRepository auditLogRepository;
  @Mock private AuditIngestionIdempotencyRepository idempotencyRepository;
  @Mock private AuditLogJdbcWriter auditLogJdbcWriter;
  @Mock private MaskingService maskingService;

  private AuditEventService service;

  @BeforeEach
  void setUp() {
    service =
        new AuditEventService(
            auditLogRepository, idempotencyRepository, auditLogJdbcWriter, maskingService);
  }

  @Test
  void persistsMaskedEventWithDerivedOperation() {
    when(maskingService.mask("secret@example.com")).thenReturn("se***@example.com");
    when(maskingService.mask(null)).thenReturn(null);

    AuditLogEntity saved = new AuditLogEntity();
    saved.setAuditLogId(42L);
    saved.setCorrelationId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
    saved.setOperation(AuditOperation.UPDATE.name());
    saved.setEventTimestamp(Instant.parse("2026-08-11T12:00:00Z"));
    when(auditLogJdbcWriter.insert(any())).thenReturn(saved);

    var response =
        service.recordEvent(
            new AuditEventRequest(
                "UPD",
                null,
                "secret@example.com",
                "KEY-1",
                "customer-svc",
                "CUS001A",
                "user001",
                "CUSTOMER_T",
                "EMAIL",
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000")));

    assertThat(response.auditLogId()).isEqualTo(42L);
    assertThat(response.operation()).isEqualTo("UPDATE");

    ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
    verify(auditLogJdbcWriter).insert(captor.capture());
    assertThat(captor.getValue().getNewValue()).isEqualTo("se***@example.com");
    assertThat(captor.getValue().getActionCd()).isEqualTo("UPD");
  }

  @Test
  void normalizeActionCdUppercasesAndTrims() {
    assertThat(AuditEventService.normalizeActionCd("  pay ")).isEqualTo("PAY");
  }
}
