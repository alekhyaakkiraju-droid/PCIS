package com.pcis.policy.batch.pol006b.infrastructure;

import com.pcis.policy.batch.pol006b.domain.RenewalResult;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.stereotype.Component;

@Component
public class PolicyRenewalSkipListener implements SkipListener<String, RenewalResult> {

  private final BatchExceptionRecorder batchExceptionRecorder;

  public PolicyRenewalSkipListener(BatchExceptionRecorder batchExceptionRecorder) {
    this.batchExceptionRecorder = batchExceptionRecorder;
  }

  @Override
  public void onSkipInRead(Throwable t) {
    persistException(null, t);
  }

  @Override
  public void onSkipInProcess(String polNbr, Throwable t) {
    persistException(polNbr, t);
  }

  @Override
  public void onSkipInWrite(RenewalResult item, Throwable t) {
    persistException(
        item != null && item.sourcePolicy() != null ? item.sourcePolicy().getPolNbr() : null, t);
  }

  private void persistException(String policyNumber, Throwable throwable) {
    StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
    if (stepExecution == null) {
      return;
    }
    batchExceptionRecorder.record(
        stepExecution.getJobExecutionId(), policyNumber, throwable);
  }
}
