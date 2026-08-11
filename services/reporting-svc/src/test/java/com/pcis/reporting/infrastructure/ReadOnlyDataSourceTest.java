package com.pcis.reporting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pcis.reporting.config.ReadOnlyViolationException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadOnlyDataSourceTest {
  private HikariDataSource hikari;
  private ReadOnlyDataSource readOnlyDataSource;

  @BeforeEach
  void setUp() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:h2:mem:reporting;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
    config.setUsername("sa");
    config.setPassword("");
    hikari = new HikariDataSource(config);
    readOnlyDataSource = new ReadOnlyDataSource(hikari, new ReadOnlyViolationLogger());
  }

  @AfterEach
  void tearDown() {
    hikari.close();
  }

  @Test
  void rejectsInsert() {
    assertThatThrownBy(
            () -> {
              try (var c = readOnlyDataSource.getConnection();
                  var ps = c.prepareStatement("INSERT INTO POLICY_T (POL_NBR) VALUES ('X')")) {
                ps.executeUpdate();
              }
            })
        .isInstanceOf(ReadOnlyViolationException.class);
  }

  @Test
  void allowsSelect() throws Exception {
    try (var c = readOnlyDataSource.getConnection();
        var ps = c.prepareStatement("SELECT 1");
        var rs = ps.executeQuery()) {
      assertThat(rs.next()).isTrue();
    }
  }
}
