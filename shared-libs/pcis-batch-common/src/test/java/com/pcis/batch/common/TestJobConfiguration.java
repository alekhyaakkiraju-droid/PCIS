package com.pcis.batch.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Minimal Spring Batch job wiring for batch-common integration tests. */
public final class TestJobConfiguration {

  private TestJobConfiguration() {}

  public static Job demoOutboxJob(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      DataSource dataSource,
      BatchRunLogTasklet runLogTasklet) {
    Step processStep = processStep(jobRepository, transactionManager, dataSource);
    Step runLogStep =
        BatchJobRunLogSupport.runLogStep("runLogStep", jobRepository, transactionManager, runLogTasklet);
    return new JobBuilder("demoOutboxJob", jobRepository).start(processStep).next(runLogStep).build();
  }

  private static Step processStep(
      JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      DataSource dataSource) {
    JdbcCursorItemReader<String> reader =
        new JdbcCursorItemReaderBuilder<String>()
            .name("demoReader")
            .dataSource(dataSource)
            .sql("SELECT value_text FROM batch_demo_t ORDER BY id")
            .rowMapper((rs, rowNum) -> rs.getString("value_text"))
            .build();

    JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
    OutboxEventWriter outboxEventWriter = new OutboxEventWriter(jdbcTemplate, new ObjectMapper(), "DEMO");
    ItemWriter<String> delegate =
        chunk -> {
          for (String item : chunk.getItems()) {
            jdbcTemplate.update("INSERT INTO batch_demo_t (value_text) VALUES (?)", "out-" + item);
          }
        };
    OutboxEnlistingItemWriter<String> writer =
        new OutboxEnlistingItemWriter<>(
            delegate,
            outboxEventWriter,
            item ->
                new OutboxEventSpec(
                    "demo",
                    item,
                    "DemoProcessed",
                    Map.of("value", item),
                    UUID.nameUUIDFromBytes(item.getBytes())));

    return new StepBuilder("processStep", jobRepository)
        .<String, String>chunk(1, transactionManager)
        .reader(reader)
        .writer(writer)
        .build();
  }
}
