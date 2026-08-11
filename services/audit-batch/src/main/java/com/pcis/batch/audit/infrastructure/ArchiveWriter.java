package com.pcis.batch.audit.infrastructure;

import com.pcis.batch.audit.config.AuditArchiveProperties;
import com.pcis.batch.audit.domain.AuditLogRow;
import com.pcis.batch.common.OutboxEventWriter;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class ArchiveWriter implements ItemWriter<AuditLogRow> {

  public static final String ARCHIVED_COUNT_KEY = "archivedCount";
  public static final String DELETED_COUNT_KEY = "deletedCount";

  private static final String INSERT_ARCHIVE =
      """
      INSERT INTO AUDIT_LOG_ARCHIVE_T (
          LOG_ID,
          PROGRAM_NAME,
          ACTION_CODE,
          TABLE_NAME,
          RECORD_KEY,
          USER_ID,
          LOG_TIMESTAMP,
          ARCHIVE_DATE,
          CRT_USER,
          CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String DELETE_SOURCE =
      """
      DELETE FROM AUDIT_LOG_T
      WHERE LOG_ID = ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final OutboxEventWriter outboxEventWriter;
  private final AuditArchiveProperties properties;
  private StepExecution stepExecution;

  public ArchiveWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter outboxEventWriter,
      AuditArchiveProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.outboxEventWriter = outboxEventWriter;
    this.properties = properties;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;
  }

  @Override
  @Transactional
  public void write(Chunk<? extends AuditLogRow> chunk) {
    List<? extends AuditLogRow> items = chunk.getItems();
    if (items.isEmpty()) {
      return;
    }

    LocalDate archiveDate = LocalDate.now();
    Instant now = Instant.now();
    int archived = 0;
    int deleted = 0;

    for (AuditLogRow row : items) {
      int inserted =
          jdbcTemplate.update(
              INSERT_ARCHIVE,
              row.logId(),
              row.programName(),
              row.actionCode(),
              row.tableName(),
              row.recordKey(),
              row.userId(),
              Timestamp.from(row.logTimestamp()),
              Date.valueOf(archiveDate),
              properties.getProgramName(),
              Timestamp.from(now));
      if (inserted == 1) {
        archived++;
      }
      deleted += jdbcTemplate.update(DELETE_SOURCE, row.logId());
    }

    writeChunkArchivedOutbox(items, archiveDate, archived);
    incrementCounter(ARCHIVED_COUNT_KEY, archived);
    incrementCounter(DELETED_COUNT_KEY, deleted);
  }

  private void writeChunkArchivedOutbox(
      List<? extends AuditLogRow> items, LocalDate archiveDate, int archived) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("program", properties.getProgramName());
    payload.put("jobName", "auditArchiveJob");
    payload.put("archiveDate", archiveDate.toString());
    payload.put("archivedCount", archived);
    payload.put("logIds", items.stream().map(AuditLogRow::logId).toList());

    outboxEventWriter.write(
        "audit-archive",
        "chunk-" + stepExecution.getId(),
        "ChunkArchived",
        payload,
        UUID.randomUUID());
  }

  private void incrementCounter(String key, int delta) {
    if (stepExecution == null || delta == 0) {
      return;
    }
    long current = stepExecution.getExecutionContext().getLong(key, 0L);
    stepExecution.getExecutionContext().putLong(key, current + delta);
  }
}
