package com.pcis.audit.application;

import com.pcis.audit.contract.AuditEventRequest;
import com.pcis.audit.contract.AuditEventResponse;
import com.pcis.audit.contract.AuditEventValidator;
import com.pcis.audit.contract.ValidatedAuditEvent;
import com.pcis.audit.infrastructure.masking.MaskingService;
import com.pcis.audit.infrastructure.persistence.entity.AuditLogEntity;
import com.pcis.audit.infrastructure.persistence.repository.AuditLogRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

  private final AuditLogRepository auditLogRepository;
  private final MaskingService maskingService;

  public AuditEventService(AuditLogRepository auditLogRepository, MaskingService maskingService) {
    this.auditLogRepository = auditLogRepository;
    this.maskingService = maskingService;
  }

  @Transactional
  public AuditEventResponse recordEvent(AuditEventRequest request) {
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

    AuditLogEntity saved = auditLogRepository.save(entity);

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
