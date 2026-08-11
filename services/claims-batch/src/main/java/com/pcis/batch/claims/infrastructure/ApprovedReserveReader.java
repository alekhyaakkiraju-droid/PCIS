package com.pcis.batch.claims.infrastructure;

import com.pcis.batch.claims.domain.ApprovedReserveRow;
import javax.sql.DataSource;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;

public class ApprovedReserveReader extends JdbcCursorItemReader<ApprovedReserveRow> {

  private static final String SELECT_APPROVED =
      """
      SELECT RESERVE_HIST_ID, CLAIM_ID, RESERVE_AMT
      FROM CLAIM_RESERVE_T
      WHERE RESERVE_STATUS = 'AP'
      ORDER BY CLAIM_ID, RESERVE_HIST_ID
      """;

  private static final RowMapper<ApprovedReserveRow> ROW_MAPPER =
      (rs, rowNum) ->
          new ApprovedReserveRow(
              rs.getLong("RESERVE_HIST_ID"),
              rs.getString("CLAIM_ID"),
              rs.getBigDecimal("RESERVE_AMT"));

  public ApprovedReserveReader(DataSource dataSource) {
    setName("approvedReserveReader");
    setDataSource(dataSource);
    setSql(SELECT_APPROVED);
    setRowMapper(ROW_MAPPER);
  }
}
