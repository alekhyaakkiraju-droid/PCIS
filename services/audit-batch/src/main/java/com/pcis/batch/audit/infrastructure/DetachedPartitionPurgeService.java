package com.pcis.batch.audit.infrastructure;

import com.pcis.batch.audit.config.AuditArchiveProperties;
import com.pcis.batch.audit.config.RetentionConfigService;
import com.pcis.batch.audit.domain.PurgeType;
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

@Component
public class DetachedPartitionPurgeService {

  private static final Logger log = LoggerFactory.getLogger(DetachedPartitionPurgeService.class);
  private static final Pattern PARTITION_NAME =
      Pattern.compile("^audit_log_t_y(\\d{4})m(\\d{2})$", Pattern.CASE_INSENSITIVE);

  private final JdbcTemplate jdbcTemplate;
  private final RetentionConfigService retentionConfigService;
  private final AuditArchiveProperties archiveProperties;
  private final PurgeEvidenceWriter purgeEvidenceWriter;

  public DetachedPartitionPurgeService(
      JdbcTemplate jdbcTemplate,
      RetentionConfigService retentionConfigService,
      AuditArchiveProperties archiveProperties,
      PurgeEvidenceWriter purgeEvidenceWriter) {
    this.jdbcTemplate = jdbcTemplate;
    this.retentionConfigService = retentionConfigService;
    this.archiveProperties = archiveProperties;
    this.purgeEvidenceWriter = purgeEvidenceWriter;
  }

  public int purgeEligibleDetachedPartitions(Instant now, String actor) {
    List<String> detached = listDetachedPartitions();
    int purged = 0;
    for (String partition : detached) {
      try {
        if (purgePartitionIfEligible(partition, now, actor)) {
          purged++;
        }
      } catch (RuntimeException ex) {
        log.error(
            "Partition purge failed partition={} error={}",
            partition,
            ex.getMessage(),
            ex);
      }
    }
    return purged;
  }

  public boolean isEligibleForPurge(String partitionName, Instant now) {
    Instant partitionEnd = partitionEndInstant(partitionName);
    if (partitionEnd == null) {
      return false;
    }
    int retentionDays = resolveRetentionDays(partitionName);
    Instant eligibleAt = partitionEnd.plus(retentionDays, java.time.temporal.ChronoUnit.DAYS);
    Instant floorEligibleAt =
        partitionEnd.plus(
            RetentionConfigService.POLICY_MINIMUM_RETENTION_DAYS,
            java.time.temporal.ChronoUnit.DAYS);
    Instant effectiveEligibleAt =
        eligibleAt.isAfter(floorEligibleAt) ? eligibleAt : floorEligibleAt;
    return !now.isBefore(effectiveEligibleAt);
  }

  private boolean purgePartitionIfEligible(String partitionName, Instant now, String actor) {
    if (!isEligibleForPurge(partitionName, now)) {
      log.debug("Partition {} not yet eligible for purge at {}", partitionName, now);
      return false;
    }
    int retentionDays = resolveRetentionDays(partitionName);
    String tier = resolveDominantTier(partitionName);
    jdbcTemplate.execute("DROP TABLE IF EXISTS " + partitionName + " CASCADE");
    purgeEvidenceWriter.recordPurge(
        PurgeType.PARTITION_DROP,
        partitionName,
        tier,
        retentionDays,
        now,
        actor,
        null);
    log.info("Dropped detached audit partition {}", partitionName);
    return true;
  }

  private int resolveRetentionDays(String partitionName) {
    int maxTierDays = 0;
    for (int days : archiveProperties.getRetention().allTierDays()) {
      maxTierDays = Math.max(maxTierDays, days);
    }
    return Math.max(maxTierDays, RetentionConfigService.POLICY_MINIMUM_RETENTION_DAYS);
  }

  private String resolveDominantTier(String partitionName) {
    int maxDays = 0;
    String dominant = "INTERNAL";
    for (String tier : List.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "RESTRICTED")) {
      int days = retentionConfigService.getRetentionDaysForTier(tier);
      if (days >= maxDays) {
        maxDays = days;
        dominant = tier;
      }
    }
    return dominant;
  }

  private List<String> listDetachedPartitions() {
    return jdbcTemplate.queryForList(
        """
        SELECT c.relname
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = current_schema()
          AND c.relkind = 'r'
          AND c.relname ~ '^audit_log_t_y[0-9]{4}m[0-9]{2}$'
          AND NOT EXISTS (
              SELECT 1
              FROM pg_inherits i
              JOIN pg_class parent ON i.inhparent = parent.oid
              WHERE i.inhrelid = c.oid
                AND parent.relname = 'audit_log_t'
          )
        ORDER BY c.relname
        """,
        String.class);
  }

  private static Instant partitionEndInstant(String partitionName) {
    Matcher matcher = PARTITION_NAME.matcher(partitionName.toLowerCase());
    if (!matcher.matches()) {
      return null;
    }
    YearMonth month =
        YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    LocalDate partitionEnd = month.plusMonths(1).atDay(1);
    return partitionEnd.atStartOfDay().toInstant(ZoneOffset.UTC);
  }
}
