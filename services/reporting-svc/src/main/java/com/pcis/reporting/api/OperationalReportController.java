package com.pcis.reporting.api;

import com.pcis.reporting.api.dto.AuditArchiveStatsResponse;
import com.pcis.reporting.api.dto.OperationalSummaryResponse;
import com.pcis.reporting.application.OperationalReportService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports/operational")
@ConditionalOnProperty(prefix = "pcis.reporting.datasource", name = "url")
@ConditionalOnBean(OperationalReportService.class)
public class OperationalReportController {

  private final OperationalReportService operationalReportService;

  public OperationalReportController(OperationalReportService operationalReportService) {
    this.operationalReportService = operationalReportService;
  }

  @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
  public OperationalSummaryResponse summary() {
    return operationalReportService.operationalSummary();
  }

  @GetMapping(value = "/audit-archive", produces = MediaType.APPLICATION_JSON_VALUE)
  public AuditArchiveStatsResponse auditArchive() {
    return operationalReportService.auditArchiveStats();
  }
}
