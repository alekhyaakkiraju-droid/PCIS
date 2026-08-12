package com.pcis.configsvc.batch;

import java.time.Instant;
import java.util.List;

public record BatchJobRunResponse(
    String jobName,
    String domain,
    Long jobExecutionId,
    Instant startTime,
    Instant endTime,
    String status,
    String exitCode,
    long readCount,
    long writeCount,
    long skipCount,
    List<BatchStepResponse> steps) {

  public record BatchStepResponse(
      String stepName,
      String status,
      long readCount,
      long writeCount,
      long skipCount,
      String exitCode) {}
}
