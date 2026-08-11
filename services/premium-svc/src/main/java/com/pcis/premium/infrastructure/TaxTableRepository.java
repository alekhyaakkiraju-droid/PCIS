package com.pcis.premium.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TaxTableRepository {

  private final JdbcClient jdbcClient;

  public TaxTableRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  public List<TaxRow> findEffectiveTaxes(String stateCode, LocalDate asOfDate) {
    return jdbcClient
        .sql(
            """
            SELECT tax_table_id, state, tax_pct, tax_type, flat_fee, compound_flag, calc_sequence
            FROM tax_table_t
            WHERE TRIM(state) = :stateCode
              AND eff_date <= :asOfDate
            ORDER BY calc_sequence, tax_table_id
            """)
        .param("stateCode", stateCode)
        .param("asOfDate", asOfDate)
        .query(
            (rs, rowNum) ->
                new TaxRow(
                    rs.getLong("tax_table_id"),
                    rs.getString("state").trim(),
                    rs.getBigDecimal("tax_pct"),
                    rs.getString("tax_type"),
                    rs.getBigDecimal("flat_fee"),
                    rs.getBoolean("compound_flag"),
                    rs.getInt("calc_sequence")))
        .list();
  }

  public record TaxRow(
      long taxId,
      String stateCode,
      BigDecimal taxPct,
      String taxType,
      BigDecimal flatFee,
      boolean compound,
      int calcSequence) {}
}
