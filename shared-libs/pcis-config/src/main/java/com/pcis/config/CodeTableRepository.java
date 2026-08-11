package com.pcis.config;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/** Internal JDBC access to CODE_TABLE_T from pcis-schema; not exposed to domain services. */
public class CodeTableRepository {

  private static final RowMapper<CodeTableEntry> ROW_MAPPER =
      (rs, rowNum) ->
          new CodeTableEntry(
              rs.getString("code_type"),
              rs.getString("code_value"),
              rs.getString("code_desc"),
              "Y".equalsIgnoreCase(rs.getString("active_flag")));

  private final JdbcTemplate jdbcTemplate;

  public CodeTableRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public CodeTableEntry findByDomainAndCode(String domainCode, String codeValue) {
    List<CodeTableEntry> rows =
        jdbcTemplate.query(
            """
            SELECT code_type, code_value, code_desc, active_flag
            FROM code_table_t
            WHERE code_type = ?
              AND code_value = ?
            """,
            ROW_MAPPER,
            domainCode,
            codeValue);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public List<CodeTableEntry> findActiveByDomain(String domainCode) {
    return jdbcTemplate.query(
        """
        SELECT code_type, code_value, code_desc, active_flag
        FROM code_table_t
        WHERE code_type = ?
          AND active_flag = 'Y'
        ORDER BY code_value
        """,
        ROW_MAPPER,
        domainCode);
  }

  public boolean isActiveMember(String domainCode, String codeValue) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM code_table_t
            WHERE code_type = ?
              AND code_value = ?
              AND active_flag = 'Y'
            """,
            Integer.class,
            domainCode,
            codeValue);
    return count != null && count > 0;
  }
}
