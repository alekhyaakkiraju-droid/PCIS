package com.pcis.batch.policy.infrastructure;

import com.pcis.batch.common.OutboxEventWriter;
import com.pcis.batch.policy.config.PolicyRenewalProperties;
import com.pcis.batch.policy.domain.RenewalDecision;
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

public class RenewalPolicyWriter implements ItemWriter<RenewalDecision> {

  public static final String SELECTED_COUNT_KEY = "selectedCount";
  public static final String RENEWED_COUNT_KEY = "renewedCount";

  private static final String INSERT_POLICY =
      """
      INSERT INTO POLICY_T (
          POL_NBR, CUST_ID, AGT_ID, POLICY_TYPE, POL_STATUS, EFF_DATE, EXP_DATE,
          PREM_ANNUAL, RENEWAL_OF_POL, BILL_FREQ, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String SELECT_COVERAGES =
      """
      SELECT COVERAGE_ID, COV_TYPE, LIMIT_AMT, DED_AMT, COV_PREMIUM
      FROM COVERAGE_T
      WHERE POL_NBR = ?
      ORDER BY COVERAGE_ID
      """;

  private static final String INSERT_COVERAGE =
      """
      INSERT INTO COVERAGE_T (
          COVERAGE_ID, POL_NBR, COV_TYPE, LIMIT_AMT, DED_AMT, COV_PREMIUM, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?)
      """;

  private static final String SELECT_DEDUCTIBLES =
      """
      SELECT DED_AMT, DED_TYPE
      FROM DEDUCTIBLE_T
      WHERE COVERAGE_ID = ?
      ORDER BY DEDUCT_ID
      """;

  private static final String INSERT_DEDUCTIBLE =
      """
      INSERT INTO DEDUCTIBLE_T (COVERAGE_ID, DED_AMT, DED_TYPE, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?)
      """;

  private static final String INSERT_HISTORY =
      """
      INSERT INTO POLICY_HISTORY_T (
          POL_NBR, EVENT_CODE, EVENT_DATE, EVENT_DESC, CRT_USER, CRT_TIMESTAMP)
      VALUES (?, ?, ?, ?, ?, ?)
      """;

  private final JdbcTemplate jdbcTemplate;
  private final OutboxEventWriter outboxEventWriter;
  private final PolicyRenewalProperties properties;
  private StepExecution stepExecution;

  public RenewalPolicyWriter(
      JdbcTemplate jdbcTemplate,
      OutboxEventWriter outboxEventWriter,
      PolicyRenewalProperties properties) {
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
  public void write(Chunk<? extends RenewalDecision> chunk) {
    for (RenewalDecision decision : chunk.getItems()) {
      writeSingle(decision);
      incrementCounter(RENEWED_COUNT_KEY);
    }
  }

  private void writeSingle(RenewalDecision decision) {
    var source = decision.source();
    Instant now = Instant.now();
    String operator = properties.getProgramName();
    String polStatus = decision.referralFlag() ? "REFR" : "RNWL";

    jdbcTemplate.update(
        INSERT_POLICY,
        decision.newPolNbr(),
        source.custId(),
        source.agtId(),
        source.policyType(),
        polStatus,
        Date.valueOf(decision.newEffDate()),
        Date.valueOf(decision.newExpDate()),
        decision.newPremium(),
        source.polNbr(),
        source.billFreq(),
        operator,
        Timestamp.from(now));

    List<Map<String, Object>> coverages =
        jdbcTemplate.queryForList(SELECT_COVERAGES, source.polNbr());
    for (Map<String, Object> coverage : coverages) {
      String oldCoverageId = (String) coverage.get("COVERAGE_ID");
      String newCoverageId = deriveCoverageId(decision.newPolNbr(), oldCoverageId);
      jdbcTemplate.update(
          INSERT_COVERAGE,
          newCoverageId,
          decision.newPolNbr(),
          coverage.get("COV_TYPE"),
          coverage.get("LIMIT_AMT"),
          coverage.get("DED_AMT"),
          coverage.get("COV_PREMIUM"),
          operator,
          Timestamp.from(now));

      // P-P8 fix: carry forward deductibles tied to each coverage (legacy POL006B gap).
      List<Map<String, Object>> deductibles =
          jdbcTemplate.queryForList(SELECT_DEDUCTIBLES, oldCoverageId);
      for (Map<String, Object> deductible : deductibles) {
        jdbcTemplate.update(
            INSERT_DEDUCTIBLE,
            newCoverageId,
            deductible.get("DED_AMT"),
            deductible.get("DED_TYPE"),
            operator,
            Timestamp.from(now));
      }
    }

    jdbcTemplate.update(
        INSERT_HISTORY,
        decision.newPolNbr(),
        "RENEW",
        Date.valueOf(LocalDate.now()),
        "Renewal of " + source.polNbr(),
        operator,
        Timestamp.from(now));

    writeRenewalOutbox(decision);
  }

  private void writeRenewalOutbox(RenewalDecision decision) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("program", properties.getProgramName());
    payload.put("sourcePolicy", decision.source().polNbr());
    payload.put("renewalPolicy", decision.newPolNbr());
    payload.put("newPremium", decision.newPremium());
    payload.put("referralFlag", decision.referralFlag());

    outboxEventWriter.write(
        "policy-renewal",
        decision.newPolNbr(),
        "PolicyRenewed",
        payload,
        UUID.randomUUID());
  }

  static String deriveCoverageId(String newPolNbr, String oldCoverageId) {
    String suffix = oldCoverageId.length() > 4 ? oldCoverageId.substring(oldCoverageId.length() - 4) : oldCoverageId;
    String candidate = newPolNbr + suffix;
    return candidate.length() <= 14 ? candidate : candidate.substring(0, 14);
  }

  private void incrementCounter(String key) {
    if (stepExecution == null) {
      return;
    }
    var jobContext = stepExecution.getJobExecution().getExecutionContext();
    long current = jobContext.getLong(key, 0L);
    jobContext.putLong(key, current + 1);
  }
}
