package com.pcis.classification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads config/pcis-data-classification.yaml at startup, validates drift against
 * information_schema, upserts rows into data_classification, and refreshes the in-memory registry.
 */
public class DataClassificationLoader implements ApplicationRunner {

  private static final Set<String> EXCLUDED_TABLES =
      Set.of(
          "FLYWAY_SCHEMA_HISTORY",
          "DATA_CLASSIFICATION",
          "DATA_CLASSIFICATION_TIER");

  private static final String SCHEMA_COLUMNS_SQL =
      """
      SELECT c.table_name, c.column_name
      FROM information_schema.columns c
      JOIN pg_class pc ON pc.relname = c.table_name
      JOIN pg_namespace pn ON pn.oid = pc.relnamespace AND pn.nspname = c.table_schema
      WHERE c.table_schema = 'public'
        AND pc.relkind IN ('r', 'p')
        AND NOT EXISTS (SELECT 1 FROM pg_inherits i WHERE i.inhrelid = pc.oid)
      ORDER BY c.table_name, c.ordinal_position
      """;

  private final JdbcTemplate jdbcTemplate;
  private final DataClassificationProperties properties;
  private final DataClassificationRegistry registry;
  private final ResourceLoader resourceLoader;

  public DataClassificationLoader(
      JdbcTemplate jdbcTemplate,
      DataClassificationProperties properties,
      DataClassificationRegistry registry,
      ResourceLoader resourceLoader) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
    this.registry = registry;
    this.resourceLoader = resourceLoader;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Resource resource = resourceLoader.getResource(properties.getRegistryLocation());
    if (!resource.exists()) {
      throw new ClassificationRegistryException(
          "Classification registry not found at " + properties.getRegistryLocation());
    }

    ClassificationRegistryDocument document = ClassificationRegistryParser.parse(resource);
    List<ClassificationEntry> entries = ClassificationRegistryValidator.validateAndFlatten(document);

    if (properties.isDriftDetectionEnabled()) {
      detectDrift(entries);
    }

    if (properties.isLoaderEnabled()) {
      upsertTierRules(document);
      upsertEntries(document.registryVersion(), entries);
    }

    registry.replaceAll(entries);
  }

  void detectDrift(List<ClassificationEntry> entries) {
    detectDrift(entries, loadSchemaColumns());
  }

  void detectDrift(List<ClassificationEntry> entries, Map<String, Set<String>> schemaColumns) {
    Map<String, Set<String>> registryColumns = new HashMap<>();
    for (ClassificationEntry entry : entries) {
      registryColumns
          .computeIfAbsent(entry.entityName(), ignored -> new HashSet<>())
          .add(entry.columnName());
    }

    Set<String> registryEntities = registryColumns.keySet();
    Set<String> schemaEntities = schemaColumns.keySet();

    Set<String> missingInRegistry = new HashSet<>(schemaEntities);
    missingInRegistry.removeAll(registryEntities);

    Set<String> extraInRegistry = new HashSet<>(registryEntities);
    extraInRegistry.removeAll(schemaEntities);

    if (!missingInRegistry.isEmpty() || !extraInRegistry.isEmpty()) {
      throw new ClassificationDriftException(
          "Entity drift between registry and schema. Missing in registry: "
              + missingInRegistry
              + "; Extra in registry: "
              + extraInRegistry);
    }

    var columnDrift = new StringBuilder();
    for (String entity : registryEntities) {
      Set<String> registryCols = registryColumns.get(entity);
      Set<String> schemaCols = schemaColumns.get(entity);

      Set<String> missingCols = new HashSet<>(schemaCols);
      missingCols.removeAll(registryCols);

      Set<String> extraCols = new HashSet<>(registryCols);
      extraCols.removeAll(schemaCols);

      if (!missingCols.isEmpty() || !extraCols.isEmpty()) {
        columnDrift
            .append(entity)
            .append(" missing=")
            .append(missingCols)
            .append(" extra=")
            .append(extraCols)
            .append("; ");
      }
    }

    if (!columnDrift.isEmpty()) {
      throw new ClassificationDriftException(
          "Column drift between registry and schema: " + columnDrift);
    }
  }

  private Map<String, Set<String>> loadSchemaColumns() {
    Map<String, Set<String>> schema = new HashMap<>();
    jdbcTemplate.query(
        SCHEMA_COLUMNS_SQL,
        (ResultSet rs) -> {
          while (rs.next()) {
            String table = rs.getString("table_name").toUpperCase();
            if (EXCLUDED_TABLES.contains(table)) {
              continue;
            }
            String column = rs.getString("column_name").toUpperCase();
            schema.computeIfAbsent(table, ignored -> new HashSet<>()).add(column);
          }
          return null;
        });
    return schema;
  }

  private void upsertTierRules(ClassificationRegistryDocument document) {
    if (document.tierHandling() == null) {
      return;
    }
    document
        .tierHandling()
        .forEach(
            (tierName, rule) -> {
              DataTier tier = DataTier.fromYaml(tierName);
              jdbcTemplate.update(
                  """
                  INSERT INTO data_classification_tier
                      (tier, retention_days, storage_encryption, access_control, log_emission, registry_version)
                  VALUES (?, ?, ?, ?, ?, ?)
                  ON CONFLICT (tier) DO UPDATE SET
                      retention_days = EXCLUDED.retention_days,
                      storage_encryption = EXCLUDED.storage_encryption,
                      access_control = EXCLUDED.access_control,
                      log_emission = EXCLUDED.log_emission,
                      registry_version = EXCLUDED.registry_version,
                      loaded_at = CURRENT_TIMESTAMP
                  """,
                  tier.yamlValue(),
                  rule.retentionDays(),
                  rule.storageEncryption(),
                  rule.accessControl(),
                  rule.logEmission(),
                  document.registryVersion());
            });
  }

  private void upsertEntries(String registryVersion, List<ClassificationEntry> entries) {
    for (ClassificationEntry entry : entries) {
      jdbcTemplate.update(
          """
          INSERT INTO data_classification
              (entity_name, column_name, data_tier, mask_strategy, retention_days,
               pii, discriminator_column, rationale, registry_version)
          VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
          ON CONFLICT (entity_name, column_name) DO UPDATE SET
              data_tier = EXCLUDED.data_tier,
              mask_strategy = EXCLUDED.mask_strategy,
              retention_days = EXCLUDED.retention_days,
              pii = EXCLUDED.pii,
              discriminator_column = EXCLUDED.discriminator_column,
              rationale = EXCLUDED.rationale,
              registry_version = EXCLUDED.registry_version,
              loaded_at = CURRENT_TIMESTAMP
          """,
          entry.entityName(),
          entry.columnName(),
          entry.tier().yamlValue(),
          entry.maskStrategy().name(),
          entry.retentionDays(),
          entry.pii(),
          entry.discriminatorColumn(),
          entry.rationale(),
          registryVersion);
    }
  }
}
