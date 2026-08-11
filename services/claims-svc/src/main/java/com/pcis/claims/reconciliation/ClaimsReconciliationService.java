package com.pcis.claims.reconciliation;

import com.pcis.claims.domain.ClaimPaymentEntity;
import com.pcis.claims.domain.repository.ClaimPaymentRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Compares legacy Db2 payment extracts to PostgreSQL {@code claim_payment} rows for parallel-run
 * cutover gating (WO-200).
 */
@Service
public class ClaimsReconciliationService {

  private final ClaimPaymentRepository claimPaymentRepository;

  public ClaimsReconciliationService(ClaimPaymentRepository claimPaymentRepository) {
    this.claimPaymentRepository = claimPaymentRepository;
  }

  public ReconciliationReport reconcile(InputStream legacyCsv) throws IOException {
    Map<String, BigDecimal> legacyTotals = readLegacyExtract(legacyCsv);
    Map<String, BigDecimal> targetTotals = loadTargetTotals();
    return compareTotals(legacyTotals, targetTotals);
  }

  public ReconciliationReport reconcile(Map<String, BigDecimal> legacyTotals) {
    return compareTotals(legacyTotals, loadTargetTotals());
  }

  private Map<String, BigDecimal> readLegacyExtract(InputStream legacyCsv) throws IOException {
    Map<String, BigDecimal> totals = new HashMap<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(legacyCsv, StandardCharsets.UTF_8))) {
      String line;
      boolean headerSkipped = false;
      while ((line = reader.readLine()) != null) {
        if (!StringUtils.hasText(line)) {
          continue;
        }
        if (!headerSkipped && line.toLowerCase().contains("claim_nbr")) {
          headerSkipped = true;
          continue;
        }
        String[] parts = line.split(",", -1);
        if (parts.length < 2) {
          continue;
        }
        String claimNbr = parts[0].trim();
        BigDecimal amount = new BigDecimal(parts[1].trim());
        totals.merge(claimNbr, amount, BigDecimal::add);
      }
    }
    return totals;
  }

  private Map<String, BigDecimal> loadTargetTotals() {
    Map<String, BigDecimal> totals = new HashMap<>();
    for (ClaimPaymentEntity payment : claimPaymentRepository.findAll()) {
      String claimNbr = payment.getClaim().getClaimNbr();
      totals.merge(claimNbr, payment.getPaymentAmt(), BigDecimal::add);
    }
    return totals;
  }

  private ReconciliationReport compareTotals(
      Map<String, BigDecimal> legacyTotals, Map<String, BigDecimal> targetTotals) {
    List<ReconciliationBreak> breaks = new ArrayList<>();
    long matched = 0;

    for (Map.Entry<String, BigDecimal> legacyEntry : legacyTotals.entrySet()) {
      String claimNbr = legacyEntry.getKey();
      BigDecimal legacyAmount = legacyEntry.getValue();
      BigDecimal targetAmount = targetTotals.get(claimNbr);
      if (targetAmount == null) {
        breaks.add(
            new ReconciliationBreak(
                BreakClass.MISSING_IN_TARGET,
                claimNbr,
                "payment_amt",
                legacyAmount.toPlainString(),
                null));
        continue;
      }
      if (legacyAmount.compareTo(targetAmount) != 0) {
        breaks.add(
            new ReconciliationBreak(
                BreakClass.VALUE_MISMATCH,
                claimNbr,
                "payment_amt",
                legacyAmount.toPlainString(),
                targetAmount.toPlainString()));
        continue;
      }
      matched++;
    }

    for (Map.Entry<String, BigDecimal> targetEntry : targetTotals.entrySet()) {
      if (!legacyTotals.containsKey(targetEntry.getKey())) {
        breaks.add(
            new ReconciliationBreak(
                BreakClass.MISSING_IN_LEGACY,
                targetEntry.getKey(),
                "payment_amt",
                null,
                targetEntry.getValue().toPlainString()));
      }
    }

    if (breaks.isEmpty()) {
      return ReconciliationReport.pass(legacyTotals.size(), targetTotals.size(), matched);
    }
    return ReconciliationReport.fail(
        legacyTotals.size(), targetTotals.size(), matched, breaks);
  }
}
