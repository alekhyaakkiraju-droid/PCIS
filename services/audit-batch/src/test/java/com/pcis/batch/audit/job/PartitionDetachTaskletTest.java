package com.pcis.batch.audit.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.batch.audit.config.RetentionConfigService;
import com.pcis.batch.audit.infrastructure.PartitionRetentionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.test.MetaDataInstanceFactory;

@ExtendWith(MockitoExtension.class)
class PartitionDetachTaskletTest {

  @Mock private PartitionRetentionService partitionRetentionService;
  @Mock private RetentionConfigService retentionConfigService;

  private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

  @Test
  void execute_detachesExpiredPartitionsAndRecordsCount() throws Exception {
    org.mockito.Mockito.when(retentionConfigService.getRetentionDays()).thenReturn(365);
    org.mockito.Mockito.when(partitionRetentionService.detachFullyExpiredPartitions(org.mockito.Mockito.any()))
        .thenReturn(3);

    PartitionDetachTasklet tasklet =
        new PartitionDetachTasklet(partitionRetentionService, retentionConfigService, clock);
    StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution();

    RepeatStatus status =
        tasklet.execute(new StepContribution(stepExecution), chunkContext(stepExecution));

    assertThat(status).isEqualTo(RepeatStatus.FINISHED);
    assertThat(
            stepExecution
                .getJobExecution()
                .getExecutionContext()
                .getInt(PartitionDetachTasklet.DETACHED_PARTITION_COUNT_KEY))
        .isEqualTo(3);

    ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
    org.mockito.Mockito.verify(partitionRetentionService)
        .detachFullyExpiredPartitions(cutoffCaptor.capture());
    assertThat(cutoffCaptor.getValue()).isEqualTo(Instant.parse("2025-01-15T00:00:00Z"));
  }

  private static ChunkContext chunkContext(StepExecution stepExecution) {
    return new ChunkContext(new StepContext(stepExecution));
  }
}
