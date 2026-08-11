package com.pcis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigSchemaIntegrationTest {

  private static final DockerImageName PG17 =
      DockerImageName.parse("postgres:17-alpine").asCompatibleSubstituteFor("postgres");

  private PostgreSQLContainer<?> container;
  private JdbcTemplate jdbcTemplate;

  @BeforeAll
  void startDatabase() {
    org.junit.jupiter.api.Assumptions.assumeTrue(
        DockerClientFactory.instance().isDockerAvailable(), "Docker unavailable");

    container =
        new PostgreSQLContainer<>(PG17)
            .withDatabaseName("pcis_config")
            .withUsername("pcis")
            .withPassword("pcis");
    container.start();

    DataSource dataSource =
        new DriverManagerDataSource(
            container.getJdbcUrl(), container.getUsername(), container.getPassword());
    migrate(dataSource);
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Test
  void seedsTwelveTunablesIncludingBatchActors() {
    Integer count =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM config_tunable_t", Integer.class);
    assertThat(count).isEqualTo(12);

    String auditActor =
        jdbcTemplate.queryForObject(
            "SELECT value_text FROM config_tunable_t WHERE tunable_key = 'batch.actor.audit'",
            String.class);
    assertThat(auditActor).isEqualTo("BATCH_AUD");
  }

  @Test
  void seedsBillingFrequencyIntervalRuleSet() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM config_rule_set_t WHERE rule_set_key = 'billing-frequency-interval'",
            Integer.class);
    assertThat(count).isEqualTo(1);

    Integer monthlyInterval =
        jdbcTemplate.queryForObject(
            """
            SELECT (payload -> 'mappings' -> 0 ->> 'intervalMonths')::int
            FROM config_rule_set_t
            WHERE rule_set_key = 'billing-frequency-interval'
            """,
            Integer.class);
    assertThat(monthlyInterval).isEqualTo(1);
  }

  @Test
  void historyTableIsAppendOnly() {
    jdbcTemplate.update(
        """
        INSERT INTO config_tunable_history_t
            (tunable_key, version_no, old_value, new_value, change_reason, changed_by)
        VALUES ('billing.leadDays', 1, '15', '20', 'integration test', 'TEST')
        """);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE config_tunable_history_t SET new_value = '21' WHERE tunable_key = 'billing.leadDays'"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("append-only");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "DELETE FROM config_tunable_history_t WHERE tunable_key = 'billing.leadDays'"))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("append-only");
  }

  @Test
  void rejectsOverlappingEffectiveDatesForSameTunableKey() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO config_tunable_t (
                        tunable_key, domain_cd, value_type, numeric_value, min_value, max_value,
                        unit_cd, description, effective_from, effective_to, version_no,
                        crt_user, crt_timestamp
                    ) VALUES (
                        'billing.leadDays', 'BIL', 'I', 20, 1, 90, 'days',
                        'Overlapping lead days', CURRENT_DATE, NULL, 2,
                        'SYSTEM', CURRENT_TIMESTAMP
                    )
                    """))
        .isInstanceOf(DataAccessException.class)
        .satisfies(
            ex -> {
              String message = ex.getMessage();
              assertThat(message).containsAnyOf("ex_config_tunable_effective_dates", "conflicts");
            });
  }

  @Test
  void historyForeignKeyReferencesTunableVersion() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO config_tunable_history_t
                        (tunable_key, version_no, old_value, new_value, change_reason, changed_by)
                    VALUES ('missing.tunable', 99, 'a', 'b', 'bad fk', 'TEST')
                    """))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("fk_config_tunable_history_tunable");
  }

  @Test
  void flywayMigrationIsIdempotent() throws SQLException {
    DataSource dataSource =
        new DriverManagerDataSource(
            container.getJdbcUrl(), container.getUsername(), container.getPassword());
    int appliedTwice = migrate(dataSource).migrationsExecuted;
    assertThat(appliedTwice).isZero();
  }

  private static org.flywaydb.core.api.output.MigrateResult migrate(DataSource dataSource) {
    Path migrations = Path.of("db/migration").toAbsolutePath();
      if (!migrations.resolve("V100__config_tunables.sql").toFile().exists()) {
      migrations = Path.of("..", "db", "migration").toAbsolutePath();
    }
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:" + migrations)
        .load()
        .migrate();
  }
}
