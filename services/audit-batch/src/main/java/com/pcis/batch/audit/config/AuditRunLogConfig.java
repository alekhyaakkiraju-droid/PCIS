package com.pcis.batch.audit.config;

import com.pcis.batch.audit.infrastructure.ArchiveWriter;
import com.pcis.batch.common.BatchJobRunLogSupport;
import com.pcis.batch.common.BatchRunLogConfigService;
import com.pcis.batch.common.BatchRunLogCounters;
import com.pcis.batch.common.BatchRunLogTasklet;
import com.pcis.batch.common.BatchRunLogWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuditRunLogConfig {

  @Bean
  BatchRunLogTasklet auditRunLogTasklet(
      BatchRunLogWriter batchRunLogWriter,
      BatchRunLogConfigService batchRunLogConfigService,
      AuditArchiveProperties properties) {
    return BatchJobRunLogSupport.tasklet(
        batchRunLogWriter,
        batchRunLogConfigService,
        properties.getProgramName(),
        chunkContext -> {
          ExecutionContext jobContext =
              chunkContext
                  .getStepContext()
                  .getStepExecution()
                  .getJobExecution()
                  .getExecutionContext();
          long archived = jobContext.getLong(ArchiveWriter.ARCHIVED_COUNT_KEY, 0L);
          int errors = BatchJobRunLogSupport.countStepErrors(chunkContext);
          return BatchRunLogCounters.of((int) archived, (int) archived, errors);
        },
        () -> LocalDate.now(ZoneOffset.UTC));
  }
}
