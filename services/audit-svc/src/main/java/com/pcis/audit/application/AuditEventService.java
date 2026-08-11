package com.pcis.audit.application;

import com.pcis.audit.contract.AuditEventRequest;
import com.pcis.audit.contract.AuditEventResponse;
import com.pcis.audit.contract.AuditEventValidator;
import com.pcis.audit.contract.ValidatedAuditEvent;
import com.pcis.audit.infrastructure.masking.MaskingService;
import com.pcis.audit.infrastructure.persistence.AuditLogJdbcWriter;
import com.pcis.audit.infrastructure.persistence.entity.AuditIngestionIdempotencyEntity;
import com.pcis.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.pcis.audit.infrastructure.persistence.entity.AuditLogEntityId;
import com.pcis.audit.infrastructure.persistence.repository.AuditIngestionIdempotencyRepository;
import com.pcis.audit.infrastructure.persistence.repository.AuditLogRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

  private final AuditLogRepository auditLogRepository;
  private final AuditIngestionIdempotencyRepository idempotencyRepository;
  private final AuditLogJdbcWriter auditLogJdbcWriter;
  private final MaskingService maskingService;

  public AuditEventService(
      AuditLogRepository auditLogRepository,
      AuditIngestionIdempotencyRepository idempotencyRepository,
      AuditLogJdbcWriter auditLogJdbcWriter,
      MaskingService maskingService) {
    this.auditLogRepository = auditLogRepository;
    this.idempotencyRepository = idempotencyRepository;
    this.auditLogJdbcWriter = auditLogJdbcWriter;
    this.maskingService = maskingService;
  }

  @Transactional
  public AuditEventResponse recordEvent(AuditEventRequest request) {
    return persistEvent(null, request);
  }

  @Transactional
  public AuditEventResponse recordEventIdempotent(UUID idempotencyKey, AuditEventRequest request) {
    if (idempotencyKey != null) {
      var existing = idempotencyRepository.findById(idempotencyKey);
      if (existing.isPresent()) {
        return toResponse(existing.get());
      }
    }
    try {
      AuditEventResponse response = persistEvent(idempotencyKey, request);
      if (idempotencyKey != null) {
        idempotencyRepository.save(
            new AuditIngestionIdempotencyEntity(
                idempotencyKey,
                response.auditLogId(),
                response.eventTimestamp(),
                Instant.now()));
      }
      return response;
    } catch (DataIntegrityViolationException ex) {
      if (idempotencyKey == null) {
        throw ex;
      }
      return idempotencyRepository.findById(idempotencyKey).map(this::toResponse).orElseThrow(() -> ex);
    }
  }

  private AuditEventResponse toResponse(AuditIngestionIdempotencyEntity row) {
    AuditLogEntityId id = new AuditLogEntityId(row.getAuditLogId(), row.getEventTimestamp());
    AuditLogEntity entity =
        auditLogRepository.findById(id).orElseThrow(() -> new IllegalStateException("Missing audit row"));
    return new AuditEventResponse(
        entity.getAuditLogId(),
        entity.getCorrelationId(),
        entity.getOperation(),
        entity.getEventTimestamp());
  }

  private AuditEventResponse persistEvent(UUID idempotencyKey, AuditEventRequest request) {
    ValidatedAuditEvent validated = AuditEventValidator.validate(request);
    Instant eventTimestamp = Instant.now();

    AuditLogEntity entity = new AuditLogEntity();
    entity.setActionCd(normalizeActionCd(request.action()));
    entity.setOldValue(maskingService.mask(validated.oldValue()));
    entity.setNewValue(maskingService.mask(validated.newValue()));
    entity.setKeyValue(validated.key());
    entity.setFieldName(validated.fieldName());
    entity.setCorrelationId(validated.correlationId());
    entity.setServiceName(validated.service());
    entity.setProgramName(validated.program());
    entity.setActor(validated.actor());
    entity.setResourceName(validated.resource());
    entity.setOperation(validated.operation().name());
    entity.setEventTimestamp(eventTimestamp);
    entity.setIdempotencyKey(idempotencyKey);

    AuditLogEntity saved = auditLogJdbcWriter.insert(entity);

    return new AuditEventResponse(
        saved.getAuditLogId(),
        saved.getCorrelationId(),
        saved.getOperation(),
        saved.getEventTimestamp());
  }

  static String normalizeActionCd(String action) {
    if (action == null) {
      return "";
    }
    return action.trim().toUpperCase(java.util.Locale.ROOT);
  }
}
