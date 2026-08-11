package com.pcis.premium.infrastructure;

import com.pcis.premium.model.PremiumDetailLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class PremiumCalcRepository {

  private final JdbcClient jdbcClient;

  public PremiumCalcRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public long insertCalculation(
      String policyNumber,
      BigDecimal finalPremium,
      String snapshotId,
      String crtUser) {
    return jdbcClient
        .sql(
            """
            INSERT INTO premium_calc_t (pol_nbr, final_premium, calc_date, snapshot_id, crt_user, crt_timestamp)
            VALUES (:polNbr, :finalPremium, :calcDate, :snapshotId, :crtUser, :crtTimestamp)
            RETURNING calc_id
            """)
        .param("polNbr", policyNumber)
        .param("finalPremium", finalPremium)
        .param("calcDate", LocalDate.now())
        .param("snapshotId", snapshotId)
        .param("crtUser", crtUser)
        .param("crtTimestamp", LocalDateTime.now())
        .query(Long.class)
        .single();
  }

  public void insertDetailLines(long calcId, List<PremiumDetailLine> lines, String crtUser) {
    for (PremiumDetailLine line : lines) {
      jdbcClient
          .sql(
              """
              INSERT INTO premium_calc_detail_t (calc_id, component, component_amt, crt_user, crt_timestamp)
              VALUES (:calcId, :component, :amount, :crtUser, :crtTimestamp)
              """)
          .param("calcId", calcId)
          .param("component", line.code())
          .param("amount", line.amount())
          .param("crtUser", crtUser)
          .param("crtTimestamp", LocalDateTime.now())
          .update();
    }
  }

  public Optional<StoredCalculation> findBySnapshotId(String snapshotId) {
    return jdbcClient
        .sql(
            """
            SELECT calc_id, pol_nbr, final_premium, calc_date, snapshot_id
            FROM premium_calc_t
            WHERE snapshot_id = :snapshotId
            """)
        .param("snapshotId", snapshotId)
        .query(
            (rs, rowNum) ->
                new StoredCalculation(
                    rs.getLong("calc_id"),
                    rs.getString("pol_nbr"),
                    rs.getBigDecimal("final_premium"),
                    rs.getObject("calc_date", LocalDate.class),
                    rs.getString("snapshot_id")))
        .optional();
  }

  public record StoredCalculation(
      long calcId,
      String policyNumber,
      BigDecimal finalPremium,
      LocalDate calcDate,
      String snapshotId) {}
}
