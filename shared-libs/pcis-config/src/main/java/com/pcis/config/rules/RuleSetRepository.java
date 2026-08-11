package com.pcis.config.rules;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** Internal JDBC access to CONFIG_RULE_SET_T; not exposed to domain services. */
public class RuleSetRepository {

  private static final RowMapper<RuleSetRow> ROW_MAPPER =
      (rs, rowNum) ->
          new RuleSetRow(
              rs.getString("rule_set_key"),
              rs.getInt("version_no"),
              rs.getString("payload"),
              rs.getDate("effective_from").toLocalDate(),
              rs.getDate("effective_to") == null ? null : rs.getDate("effective_to").toLocalDate(),
              rs.getString("status_cd"));

  private final JdbcTemplate jdbcTemplate;

  public RuleSetRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public RuleSetRow findByKeyAndVersion(String ruleSetKey, int versionNo) {
    List<RuleSetRow> rows =
        jdbcTemplate.query(
            """
            SELECT rule_set_key, version_no, payload::text AS payload,
                   effective_from, effective_to, status_cd
            FROM config_rule_set_t
            WHERE rule_set_key = ?
              AND version_no = ?
            """,
            ROW_MAPPER,
            ruleSetKey,
            versionNo);
    return rows.isEmpty() ? null : rows.getFirst();
  }
}
