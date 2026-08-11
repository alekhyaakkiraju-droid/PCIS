package com.pcis.policy.support;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

public final class BatchTestSupport {

  private BatchTestSupport() {}

  public static StepExecution stepByName(JobExecution execution, String stepName) {
    return execution.getStepExecutions().stream()
        .filter(step -> stepName.equals(step.getStepName()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Step not found: " + stepName));
  }
}
