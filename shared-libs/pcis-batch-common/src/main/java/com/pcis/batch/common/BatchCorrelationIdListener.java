package com.pcis.batch.common;

import com.pcis.observability.MdcKeys;
import com.pcis.observability.filter.CorrelationIdFilter;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.util.StringUtils;

/**
 * Populates MDC correlation context for Spring Batch jobs so logs and outbox publishes carry the
 * same correlation id as HTTP requests.
 */
public class BatchCorrelationIdListener implements JobExecutionListener {

  public static final String CORRELATION_ID_JOB_PARAM = "correlationId";

  @Override
  public void beforeJob(JobExecution jobExecution) {
    MDC.put(MdcKeys.CORRELATION_ID, resolveCorrelationId(jobExecution));
    MDC.put(MdcKeys.JOB_ID, jobExecution.getJobInstance().getJobName());
    MDC.put(MdcKeys.RUN_ID, String.valueOf(jobExecution.getId()));
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    MDC.clear();
  }

  static String resolveCorrelationId(JobExecution jobExecution) {
    JobParameters parameters = jobExecution.getJobParameters();
    if (parameters != null) {
      String fromParam = parameters.getString(CORRELATION_ID_JOB_PARAM);
      if (StringUtils.hasText(fromParam)) {
        String candidate = fromParam.trim();
        if (CorrelationIdFilter.isSafeCorrelationId(candidate)) {
          return candidate;
        }
      }
    }
    return UUID.randomUUID().toString();
  }
}
