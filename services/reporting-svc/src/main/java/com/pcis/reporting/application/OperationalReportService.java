package com.pcis.reporting.application;

import com.pcis.reporting.api.dto.AuditArchiveStatsResponse;
import com.pcis.reporting.api.dto.OperationalSummaryResponse;
import com.pcis.reporting.infrastructure.OperationalReportRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "pcis.reporting.datasource", name = "url")
@ConditionalOnBean(OperationalReportRepository.class)
public class OperationalReportService {

  private final OperationalReportRepository repository;

  public OperationalReportService(OperationalReportRepository repository) {
    this.repository = repository;
  }

  public OperationalSummaryResponse operationalSummary() {
    return repository.fetchOperationalSummary();
  }

  public AuditArchiveStatsResponse auditArchiveStats() {
    return repository.fetchAuditArchiveStats();
  }
}
