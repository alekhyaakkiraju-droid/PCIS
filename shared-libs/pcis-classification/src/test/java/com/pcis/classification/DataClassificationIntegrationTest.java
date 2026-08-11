package com.pcis.classification;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.classification.support.ClassificationTestApplication;
import java.nio.file.Path;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots classification module against Flyway V1–V3 schema and asserts registry load.
 *
 * <p>Skipped when Docker is unavailable.
 */
@EnabledIf("dockerAvailable")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = ClassificationTestApplication.class)
@Import(DataClassificationIntegrationTest.FlywayTestConfig.class)
@ActiveProfiles("test")
class DataClassificationIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17-alpine")
          .withDatabaseName("pcis_classification_it")
          .withUsername("pcis")
          .withPassword("pcis");

  @Autowired private DataClassificationRegistry registry;
  @Autowired private JdbcTemplate jdbcTemplate;

  static boolean dockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (Throwable ex) {
      return false;
    }
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @TestConfiguration
  static class FlywayTestConfig {

    @Bean
    String flywaySchemaMigrator(DataSource dataSource) {
      Path migrations = Path.of("..", "pcis-schema", "db", "migration").toAbsolutePath().normalize();
      Flyway.configure()
          .dataSource(dataSource)
          .locations("filesystem:" + migrations)
          .load()
          .migrate();
      return "migrated";
    }

    @Bean
    DataClassificationProperties dataClassificationProperties() {
      DataClassificationProperties properties = new DataClassificationProperties();
      properties.setRegistryLocation(
          "file:"
              + Path.of("..", "..", "config", "pcis-data-classification.yaml")
                  .toAbsolutePath()
                  .normalize());
      return properties;
    }

    @Bean
    DataClassificationRegistry dataClassificationRegistry() {
      return new DataClassificationRegistry();
    }

    @Bean
    DataClassificationLoader dataClassificationLoader(
        JdbcTemplate jdbcTemplate,
        DataClassificationProperties properties,
        DataClassificationRegistry registry,
        org.springframework.core.io.ResourceLoader resourceLoader) {
      return new DataClassificationLoader(jdbcTemplate, properties, registry, resourceLoader);
    }
  }

  @Test
  void loadsRegistryAndMatchesTierCounts() {
    assertThat(registry.size()).isEqualTo(545);

    Map<DataTier, Long> registryCounts = registry.countByTier();
    Map<String, Integer> dbCounts =
        jdbcTemplate.query(
            "SELECT data_tier, COUNT(*) AS cnt FROM data_classification GROUP BY data_tier",
            (rs, rowNum) -> Map.entry(rs.getString("data_tier"), rs.getInt("cnt")))
            .stream()
            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    for (DataTier tier : DataTier.values()) {
      long expected = registryCounts.getOrDefault(tier, 0L);
      int actual = dbCounts.getOrDefault(tier.yamlValue(), 0);
      assertThat(actual)
          .as("tier count for %s", tier)
          .isEqualTo((int) expected);
    }
  }

  @Test
  void restrictedCustomerTaxIdHasLastFourMask() {
    assertThat(registry.getTier("CUSTOMER_T", "TAX_ID")).isEqualTo(DataTier.RESTRICTED);
    assertThat(registry.getMaskStrategy("CUSTOMER_T", "TAX_ID")).isEqualTo(MaskStrategy.LAST_FOUR);
    assertThat(registry.getAllRestrictedColumns()).isNotEmpty();
  }
}
