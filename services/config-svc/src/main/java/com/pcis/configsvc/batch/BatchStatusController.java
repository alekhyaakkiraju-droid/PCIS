package com.pcis.configsvc.batch;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/batch")
public class BatchStatusController {

  private final BatchStatusService batchStatusService;

  public BatchStatusController(BatchStatusService batchStatusService) {
    this.batchStatusService = batchStatusService;
  }

  @GetMapping("/runs")
  @PreAuthorize("hasAuthority('configuration-admin')")
  public List<BatchJobRunResponse> listRuns() {
    return batchStatusService.listRuns();
  }
}
