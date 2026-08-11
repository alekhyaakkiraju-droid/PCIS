package com.pcis.batch.audit.infrastructure;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Detaches fully-expired AUDIT_LOG_T monthly partitions without row-level DELETE on the parent. */
@Component
public class PartitionRetentionService {

  private static final Logger log = LoggerFactory.getLogger(PartitionRetentionService.class);
  private static final Pattern PARTITION_NAME =
      Pattern.compile("^audit_log_t_y(\\d{4})m(\\d{2})$", Pattern.CASE_INSENSITIVE);

  private final JdbcTemplate jdbcTemplate;

  public PartitionRetentionService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public int detachFullyExpiredPartitions(Instant cutoff) {
    List<String> partitions =
        jdbcTemplate.queryForList(
            """
            SELECT child.relname
            FROM pg_inherits
            JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
            JOIN pg_class child ON pg_inherits.inhrelid = child.oid
            WHERE parent.relname = 'audit_log_t'
              AND child.relname ~ '^audit_log_t_y[0-9]{4}m[0-9]{2}$'
            ORDER BY child.relname
            """,
            String.class);

    int detached = 0;
    for (String partition : partitions) {
      if (!isPartitionFullyExpired(partition, cutoff)) {
        continue;
      }
      if (countUnarchivedRows(partition) > 0) {
        log.info("Skipping detach for partition {} — unarchived rows remain", partition);
        continue;
      }
      jdbcTemplate.queryForObject(
          "SELECT detach_audit_log_t_partition(?::regclass)", Integer.class, partition);
      log.info("Detached expired audit partition {} cutoff={}", partition, cutoff);
      detached++;
    }
    return detached;
  }

  private static boolean isPartitionFullyExpired(String partitionName, Instant cutoff) {
    Matcher matcher = PARTITION_NAME.matcher(partitionName.toLowerCase());
    if (!matcher.matches()) {
      return false;
    }
    YearMonth month =
        YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    LocalDate partitionEnd = month.plusMonths(1).atDay(1);
    Instant partitionEndInstant = partitionEnd.atStartOfDay().toInstant(ZoneOffset.UTC);
    return !partitionEndInstant.isAfter(cutoff);
  }

  private int countUnarchivedRows(String partitionName) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM %s live
            WHERE NOT EXISTS (
                SELECT 1 FROM audit_log_archive_t archived WHERE archived.log_id = live.log_id
            )
            """
                .formatted(partitionName),
            Integer.class);
    return count == null ? 0 : count;
  }
}
