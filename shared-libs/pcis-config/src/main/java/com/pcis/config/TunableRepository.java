package com.pcis.config;

import com.pcis.error.ReasonCode;
import com.pcis.error.TerminalPcisException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class TunableRepository {

  private static final RowMapper<TunableRow> ROW_MAPPER =
      (rs, rowNum) ->
          new TunableRow(
              rs.getString("tunable_key"),
              rs.getString("value_type"),
              rs.getString("value_text"),
              rs.getBigDecimal("numeric_value"),
              rs.getBigDecimal("min_value"),
              rs.getBigDecimal("max_value"),
              rs.getDate("effective_from").toLocalDate(),
              rs.getDate("effective_to") == null
                  ? null
                  : rs.getDate("effective_to").toLocalDate());

  private final JdbcTemplate jdbcTemplate;

  public TunableRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public TunableRow findEffective(String key) {
    List<TunableRow> rows =
        jdbcTemplate.query(
            """
            SELECT tunable_key, value_type, value_text, numeric_value, min_value, max_value,
                   effective_from, effective_to
            FROM config_tunable_t
            WHERE tunable_key = ?
              AND effective_from <= CURRENT_DATE
              AND (effective_to IS NULL OR effective_to >= CURRENT_DATE)
            ORDER BY version_no DESC
            LIMIT 1
            """,
            ROW_MAPPER,
            key);
    return rows.isEmpty() ? null : rows.getFirst();
  }
}
