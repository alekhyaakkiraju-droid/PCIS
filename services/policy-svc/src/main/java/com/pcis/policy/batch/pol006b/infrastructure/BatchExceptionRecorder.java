package com.pcis.policy.batch.pol006b.infrastructure;

import com.pcis.policy.batch.pol006b.domain.entity.BatchExceptionEntity;
import com.pcis.policy.batch.pol006b.domain.repository.BatchExceptionRepository;
import com.pcis.policy.batch.pol006b.exception.RenewalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchExceptionRecorder {

  private final BatchExceptionRepository batchExceptionRepository;

  public BatchExceptionRecorder(BatchExceptionRepository batchExceptionRepository) {
    this.batchExceptionRepository = batchExceptionRepository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(Long jobExecutionId, String policyNumber, Throwable throwable) {
    BatchExceptionEntity record = new BatchExceptionEntity();
    record.setJobExecutionId(jobExecutionId);
    record.setPolicyNumber(policyNumber);
    if (throwable instanceof RenewalException renewalException) {
      record.setErrorType(renewalException.getReasonCode());
      record.setErrorMessage(truncate(throwable.getMessage()));
    } else {
      record.setErrorType(throwable.getClass().getSimpleName());
      record.setErrorMessage(truncate(throwable.getMessage()));
    }
    batchExceptionRepository.save(record);
  }

  private static String truncate(String message) {
    if (message == null) {
      return null;
    }
    return message.length() <= 500 ? message : message.substring(0, 500);
  }
}
