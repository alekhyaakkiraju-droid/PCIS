package com.pcis.configsvc.batch;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class BatchStatusService {

  private static final Logger log = LoggerFactory.getLogger(BatchStatusService.class);

  private static final String JOB_QUERY =
      """
      SELECT ji.job_name, je.job_execution_id, je.start_time, je.end_time, je.status, je.exit_code,
             je.exit_message,
             COALESCE(SUM(se.read_count), 0) AS read_count,
             COALESCE(SUM(se.write_count), 0) AS write_count,
             COALESCE(SUM(se.read_skip_count + se.write_skip_count + se.process_skip_count), 0) AS skip_count
      FROM batch_job_instance ji
      JOIN batch_job_execution je ON je.job_instance_id = ji.job_instance_id
      LEFT JOIN batch_step_execution se ON se.job_execution_id = je.job_execution_id
      GROUP BY ji.job_name, je.job_execution_id, je.start_time, je.end_time, je.status, je.exit_code, je.exit_message
      ORDER BY je.start_time DESC
      """;

  private static final String STEP_QUERY =
      """
      SELECT step_name, status, read_count, write_count,
             (read_skip_count + write_skip_count + process_skip_count) AS skip_count, exit_code
      FROM batch_step_execution
      WHERE job_execution_id = ?
      ORDER BY step_execution_id
      """;

  private final Map<String, JdbcTemplate> jdbcTemplates;

  public BatchStatusService(BatchStatusJdbcTemplates batchStatusJdbcTemplates) {
    this.jdbcTemplates = batchStatusJdbcTemplates.byDomain();
  }

  public List<BatchJobRunResponse> listRuns() {
    List<BatchJobRunResponse> all = new ArrayList<>();
    for (Map.Entry<String, JdbcTemplate> entry : jdbcTemplates.entrySet()) {
      String domain = entry.getKey();
      JdbcTemplate jdbcTemplate = entry.getValue();
      try {
        List<BatchJobRunResponse> runs =
            jdbcTemplate.query(JOB_QUERY, jobRowMapper(domain, jdbcTemplate));
        all.addAll(runs);
      } catch (Exception ex) {
        log.warn("Skipping batch domain '{}' — {}", domain, ex.getMessage());
      }
    }
    all.sort(Comparator.comparing(BatchJobRunResponse::startTime, Comparator.nullsLast(Comparator.reverseOrder())));
    return all;
  }

  private static final int EXIT_MESSAGE_MAX_LENGTH = 2000;

  private RowMapper<BatchJobRunResponse> jobRowMapper(String domain, JdbcTemplate jdbcTemplate) {
    return (rs, rowNum) -> {
      long jobExecutionId = rs.getLong("job_execution_id");
      List<BatchJobRunResponse.BatchStepResponse> steps =
          jdbcTemplate.query(STEP_QUERY, stepRowMapper(), jobExecutionId);
      return new BatchJobRunResponse(
          rs.getString("job_name"),
          domain,
          jobExecutionId,
          toInstant(rs.getTimestamp("start_time")),
          toInstant(rs.getTimestamp("end_time")),
          rs.getString("status"),
          rs.getString("exit_code"),
          truncate(rs.getString("exit_message")),
          rs.getLong("read_count"),
          rs.getLong("write_count"),
          rs.getLong("skip_count"),
          steps);
    };
  }

  private static String truncate(String value) {
    if (value == null || value.length() <= EXIT_MESSAGE_MAX_LENGTH) {
      return value;
    }
    return value.substring(0, EXIT_MESSAGE_MAX_LENGTH) + "… (truncated)";
  }

  private RowMapper<BatchJobRunResponse.BatchStepResponse> stepRowMapper() {
    return (rs, rowNum) ->
        new BatchJobRunResponse.BatchStepResponse(
            rs.getString("step_name"),
            rs.getString("status"),
            rs.getLong("read_count"),
            rs.getLong("write_count"),
            rs.getLong("skip_count"),
            rs.getString("exit_code"));
  }

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }
}
