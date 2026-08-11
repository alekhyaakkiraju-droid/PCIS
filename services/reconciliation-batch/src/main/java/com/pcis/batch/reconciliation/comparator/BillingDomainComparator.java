package com.pcis.batch.reconciliation.comparator;

import com.pcis.batch.reconciliation.classifier.BreakClassifierRegistry;
import com.pcis.batch.reconciliation.domain.BreakClass;
import com.pcis.batch.reconciliation.domain.DomainComparisonResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Compares legacy billing schedule snapshots to {@code BILLING_SCHEDULE_T} for parallel-run gating.
 */
@Component
public class BillingDomainComparator implements DomainComparator {

  private static final String DOMAIN = "billing";
  private static final String ENTITY = "BILLING_SCHEDULE_T";

  private final JdbcTemplate readReplicaJdbcTemplate;
  private final JdbcTemplate primaryJdbcTemplate;
  private final BreakClassifierRegistry breakClassifierRegistry;

  public BillingDomainComparator(
      @Qualifier("readReplicaJdbcTemplate") JdbcTemplate readReplicaJdbcTemplate,
      JdbcTemplate primaryJdbcTemplate,
      BreakClassifierRegistry breakClassifierRegistry) {
    this.readReplicaJdbcTemplate = readReplicaJdbcTemplate;
    this.primaryJdbcTemplate = primaryJdbcTemplate;
    this.breakClassifierRegistry = breakClassifierRegistry;
  }

  @Override
  public String domain() {
    return DOMAIN;
  }

  @Override
  public DomainComparisonResult compare(long runId, LocalDate businessDate) {
    Map<String, LegacyScheduleRow> legacyRows = loadLegacyRows(businessDate);
    Map<String, TargetScheduleRow> targetRows = loadTargetRows();
    DomainComparisonResult result = new DomainComparisonResult(DOMAIN, 1);

    if (legacyRows.size() != targetRows.size()) {
      result.addBreak(
          breakClassifierRegistry.classify(
              BreakClass.COUNT_MISMATCH,
              runId,
              DOMAIN,
              ENTITY,
              "row-count",
              "count",
              Integer.toString(legacyRows.size()),
              Integer.toString(targetRows.size()),
              null));
    }

    for (Map.Entry<String, LegacyScheduleRow> legacyEntry : legacyRows.entrySet()) {
      String key = legacyEntry.getKey();
      LegacyScheduleRow legacy = legacyEntry.getValue();
      TargetScheduleRow target = targetRows.get(key);
      result.addRowsCompared(1);

      if (target == null) {
        result.addBreak(
            breakClassifierRegistry.classify(
                BreakClass.MISSING_IN_TARGET,
                runId,
                DOMAIN,
                ENTITY,
                key,
                "business_key",
                legacy.polNbr() + "/" + legacy.installmentNbr(),
                null,
                null));
        continue;
      }

      if (!legacy.schedStatus().equals(target.schedStatus())) {
        result.addBreak(
            breakClassifierRegistry.classify(
                BreakClass.STATUS_MISMATCH,
                runId,
                DOMAIN,
                ENTITY,
                key,
                "SCHED_STATUS",
                legacy.schedStatus(),
                target.schedStatus(),
                null));
      }

      if (legacy.amtDue().compareTo(target.amtDue()) != 0) {
        result.addBreak(
            breakClassifierRegistry.classify(
                BreakClass.VALUE_MISMATCH,
                runId,
                DOMAIN,
                ENTITY,
                key,
                "AMT_DUE",
                legacy.amtDue().toPlainString(),
                target.amtDue().toPlainString(),
                null));
      }
    }

    for (Map.Entry<String, TargetScheduleRow> targetEntry : targetRows.entrySet()) {
      if (!legacyRows.containsKey(targetEntry.getKey())) {
        TargetScheduleRow target = targetEntry.getValue();
        result.addBreak(
            breakClassifierRegistry.classify(
                BreakClass.MISSING_IN_LEGACY,
                runId,
                DOMAIN,
                ENTITY,
                targetEntry.getKey(),
                "business_key",
                null,
                target.polNbr() + "/" + target.installmentNbr(),
                null));
      }
    }

    String legacyChecksum = checksum(legacyRows);
    String targetChecksum = checksumTarget(targetRows);
    if (!legacyChecksum.equals(targetChecksum) && result.breaks().isEmpty()) {
      result.addBreak(
          breakClassifierRegistry.classify(
              BreakClass.CHECKSUM_MISMATCH,
              runId,
              DOMAIN,
              ENTITY,
              "checksum",
              "amt_due_total",
              legacyChecksum,
              targetChecksum,
              null));
    }

    return result;
  }

  private Map<String, LegacyScheduleRow> loadLegacyRows(LocalDate businessDate) {
    Map<String, LegacyScheduleRow> rows = new HashMap<>();
    readReplicaJdbcTemplate.query(
        """
        SELECT pol_nbr, installment_nbr, amt_due, sched_status
        FROM legacy_snapshot.billing_schedule_snapshot
        WHERE business_date = ?
        """,
        rs -> {
          while (rs.next()) {
            String polNbr = rs.getString("pol_nbr");
            int installmentNbr = rs.getInt("installment_nbr");
            BigDecimal amtDue = rs.getBigDecimal("amt_due").setScale(2, RoundingMode.UNNECESSARY);
            String schedStatus = rs.getString("sched_status");
            rows.put(key(polNbr, installmentNbr), new LegacyScheduleRow(polNbr, installmentNbr, amtDue, schedStatus));
          }
          return null;
        },
        businessDate);
    return rows;
  }

  private Map<String, TargetScheduleRow> loadTargetRows() {
    Map<String, TargetScheduleRow> rows = new HashMap<>();
    primaryJdbcTemplate.query(
        """
        SELECT pol_nbr, installment_nbr, amt_due, sched_status
        FROM billing_schedule_t
        """,
        rs -> {
          while (rs.next()) {
            String polNbr = rs.getString("pol_nbr");
            int installmentNbr = rs.getInt("installment_nbr");
            BigDecimal amtDue = rs.getBigDecimal("amt_due").setScale(2, RoundingMode.UNNECESSARY);
            String schedStatus = rs.getString("sched_status");
            rows.put(key(polNbr, installmentNbr), new TargetScheduleRow(polNbr, installmentNbr, amtDue, schedStatus));
          }
          return null;
        });
    return rows;
  }

  private static String key(String polNbr, int installmentNbr) {
    return polNbr + ":" + installmentNbr;
  }

  private static String checksum(Map<String, LegacyScheduleRow> rows) {
    BigDecimal total =
        rows.values().stream()
            .map(LegacyScheduleRow::amtDue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.UNNECESSARY);
    return total.toPlainString();
  }

  private static String checksumTarget(Map<String, TargetScheduleRow> rows) {
    BigDecimal total =
        rows.values().stream()
            .map(TargetScheduleRow::amtDue)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.UNNECESSARY);
    return total.toPlainString();
  }

  private record LegacyScheduleRow(
      String polNbr, int installmentNbr, BigDecimal amtDue, String schedStatus) {}

  private record TargetScheduleRow(
      String polNbr, int installmentNbr, BigDecimal amtDue, String schedStatus) {}
}
