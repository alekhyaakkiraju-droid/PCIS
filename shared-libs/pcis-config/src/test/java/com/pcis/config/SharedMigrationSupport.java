package com.pcis.config;

import com.pcis.config.rules.RuleSetEvaluator;
import com.pcis.config.rules.RuleSetRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

final class SharedMigrationSupport {

  private static final String CONFIG_FLYWAY_TABLE = "config_flyway_schema_history";

  private SharedMigrationSupport() {}

  static void migrateSchemaAndConfig(DataSource dataSource) {
    migrateConfig(dataSource);
    applyCodeTableFixture(dataSource);
  }

  static org.flywaydb.core.api.output.MigrateResult migrateConfig(DataSource dataSource) {
    Path configMigrations = resolveConfigMigrations();
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:" + configMigrations)
        .table(CONFIG_FLYWAY_TABLE)
        .load()
        .migrate();
  }

  static void applyCodeTableFixture(DataSource dataSource) {
    try (var conn = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(conn, new ClassPathResource("fixtures/code-table-integration.sql"));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to apply code-table integration fixture", ex);
    }
  }

  static RuleSetEvaluator ruleSetEvaluator(DataSource dataSource, PcisCodeTableProperties properties) {
    return new RuleSetEvaluator(new RuleSetRepository(new JdbcTemplate(dataSource)), properties);
  }

  static Path resolveSchemaSeedFile() {
    Path schemaSeed =
        Path.of("../pcis-schema/db/migration/V4__seed_reference_data.sql")
            .toAbsolutePath()
            .normalize();
    if (!schemaSeed.toFile().exists()) {
      schemaSeed =
          Path.of("shared-libs", "pcis-schema", "db", "migration", "V4__seed_reference_data.sql")
              .toAbsolutePath()
              .normalize();
    }
    if (!schemaSeed.toFile().exists()) {
      schemaSeed =
          Path.of("..", "..", "pcis-schema", "db", "migration", "V4__seed_reference_data.sql")
              .toAbsolutePath()
              .normalize();
    }
    return schemaSeed;
  }

  static String readSchemaSeedSql() throws IOException {
    return java.nio.file.Files.readString(resolveSchemaSeedFile(), StandardCharsets.UTF_8);
  }

  private static Path resolveConfigMigrations() {
    Path configMigrations = Path.of("db/migration").toAbsolutePath().normalize();
    if (!configMigrations.resolve("V100__config_tunables.sql").toFile().exists()) {
      configMigrations =
          Path.of("shared-libs", "pcis-config", "db", "migration").toAbsolutePath().normalize();
    }
    if (!configMigrations.resolve("V100__config_tunables.sql").toFile().exists()) {
      configMigrations = Path.of("..", "db", "migration").toAbsolutePath().normalize();
    }
    return configMigrations;
  }
}
