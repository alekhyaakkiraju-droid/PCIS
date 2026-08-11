package com.pcis.classification;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Caffeine-backed runtime registry for entity/column classification lookups.
 *
 * <p>Populated by {@link DataClassificationLoader} after YAML validation and optional DB upsert.
 */
public class DataClassificationRegistry {

  private final Map<String, ClassificationEntry> entries = new ConcurrentHashMap<>();
  private final LoadingCache<String, Optional<ClassificationEntry>> cache =
      Caffeine.newBuilder().maximumSize(10_000).build(this::loadEntry);

  public void replaceAll(Collection<ClassificationEntry> newEntries) {
    entries.clear();
    cache.invalidateAll();
    for (ClassificationEntry entry : newEntries) {
      entries.put(entry.cacheKey(), entry);
    }
  }

  public DataTier getTier(String entityName, String columnName) {
    return requireEntry(entityName, columnName).tier();
  }

  public MaskStrategy getMaskStrategy(String entityName, String columnName) {
    return requireEntry(entityName, columnName).maskStrategy();
  }

  public int getRetentionDays(String entityName, String columnName) {
    return requireEntry(entityName, columnName).retentionDays();
  }

  public int getRetentionDays(DataTier tier) {
    return entries.values().stream()
        .filter(e -> e.tier() == tier)
        .mapToInt(ClassificationEntry::retentionDays)
        .findFirst()
        .orElseThrow(
            () ->
                new ClassificationRegistryException(
                    "No retention days configured for tier " + tier));
  }

  public Optional<ClassificationEntry> findEntry(String entityName, String columnName) {
    return cache.get(normalizeKey(entityName, columnName));
  }

  public List<ClassificationEntry> getAllRestrictedColumns() {
    return entries.values().stream()
        .filter(entry -> entry.tier() == DataTier.RESTRICTED)
        .sorted(
            (a, b) -> {
              int entityCompare = a.entityName().compareTo(b.entityName());
              return entityCompare != 0 ? entityCompare : a.columnName().compareTo(b.columnName());
            })
        .collect(Collectors.toList());
  }

  public int size() {
    return entries.size();
  }

  public Map<DataTier, Long> countByTier() {
    return entries.values().stream()
        .collect(Collectors.groupingBy(ClassificationEntry::tier, Collectors.counting()));
  }

  private ClassificationEntry requireEntry(String entityName, String columnName) {
    return findEntry(entityName, columnName)
        .orElseThrow(
            () ->
                new ClassificationRegistryException(
                    "No classification for "
                        + normalizeEntity(entityName)
                        + "."
                        + normalizeColumn(columnName)));
  }

  private Optional<ClassificationEntry> loadEntry(String cacheKey) {
    return Optional.ofNullable(entries.get(cacheKey));
  }

  static String normalizeKey(String entityName, String columnName) {
    return normalizeEntity(entityName) + ":" + normalizeColumn(columnName);
  }

  static String normalizeEntity(String entityName) {
    return entityName == null ? "" : entityName.trim().toUpperCase();
  }

  static String normalizeColumn(String columnName) {
    return columnName == null ? "" : columnName.trim().toUpperCase();
  }
}
