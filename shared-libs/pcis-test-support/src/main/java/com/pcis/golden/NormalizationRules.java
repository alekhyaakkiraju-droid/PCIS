package com.pcis.golden;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

/**
 * Parsed view of {@code golden/normalization-rules.yaml}.
 */
public final class NormalizationRules {

  private final Set<String> allowTimestamps;
  private final Set<String> allowSurrogates;
  private final Set<String> timestampSuffixes;
  private final Set<String> surrogateSuffixes;
  private final Set<String> denyMonetaryColumns;
  private final Set<String> denyStatusColumns;
  private final Set<String> statusSuffixes;
  private final Set<String> denyMonetaryTypes;
  private final Map<String, List<String>> businessKeys;
  private final String defaultReferenceDate;

  private NormalizationRules(
      Set<String> allowTimestamps,
      Set<String> allowSurrogates,
      Set<String> timestampSuffixes,
      Set<String> surrogateSuffixes,
      Set<String> denyMonetaryColumns,
      Set<String> denyStatusColumns,
      Set<String> statusSuffixes,
      Set<String> denyMonetaryTypes,
      Map<String, List<String>> businessKeys,
      String defaultReferenceDate) {
    this.allowTimestamps = Set.copyOf(allowTimestamps);
    this.allowSurrogates = Set.copyOf(allowSurrogates);
    this.timestampSuffixes = Set.copyOf(timestampSuffixes);
    this.surrogateSuffixes = Set.copyOf(surrogateSuffixes);
    this.denyMonetaryColumns = Set.copyOf(denyMonetaryColumns);
    this.denyStatusColumns = Set.copyOf(denyStatusColumns);
    this.statusSuffixes = Set.copyOf(statusSuffixes);
    this.denyMonetaryTypes = Set.copyOf(denyMonetaryTypes);
    this.businessKeys = Collections.unmodifiableMap(new LinkedHashMap<>(businessKeys));
    this.defaultReferenceDate = defaultReferenceDate;
  }

  public static NormalizationRules load(Path yamlPath) {
    try (Reader reader = Files.newBufferedReader(yamlPath)) {
      return fromYaml(reader);
    } catch (IOException e) {
      throw new ConfigurationException("Unable to read normalization rules: " + yamlPath, e);
    }
  }

  public static NormalizationRules loadFromClasspath(String resource) {
    InputStream in = NormalizationRules.class.getClassLoader().getResourceAsStream(resource);
    if (in == null) {
      throw new ConfigurationException("Classpath resource not found: " + resource);
    }
    try (InputStream stream = in) {
      return fromYaml(stream);
    } catch (IOException e) {
      throw new ConfigurationException("Unable to read classpath rules: " + resource, e);
    }
  }

  @SuppressWarnings("unchecked")
  static NormalizationRules fromYaml(InputStream in) {
    Yaml yaml = new Yaml();
    Map<String, Object> root = yaml.load(in);
    return fromMap(root);
  }

  @SuppressWarnings("unchecked")
  static NormalizationRules fromYaml(Reader reader) {
    Yaml yaml = new Yaml();
    Map<String, Object> root = yaml.load(reader);
    return fromMap(root);
  }

  @SuppressWarnings("unchecked")
  private static NormalizationRules fromMap(Map<String, Object> root) {
    Objects.requireNonNull(root, "normalization rules root");
    Map<String, Object> allow = (Map<String, Object>) root.getOrDefault("allow", Map.of());
    Map<String, Object> deny = (Map<String, Object>) root.getOrDefault("deny", Map.of());
    Map<String, Object> keys = (Map<String, Object>) root.getOrDefault("business_keys", Map.of());

    Map<String, List<String>> businessKeys = new LinkedHashMap<>();
    for (Map.Entry<String, Object> e : keys.entrySet()) {
      businessKeys.put(e.getKey().toUpperCase(Locale.ROOT), toStringList(e.getValue()));
    }

    return new NormalizationRules(
        upperSet(toStringList(allow.get("timestamps"))),
        upperSet(toStringList(allow.get("surrogates"))),
        upperSet(toStringList(allow.get("timestamp_suffixes"))),
        upperSet(toStringList(allow.get("surrogate_suffixes"))),
        upperSet(toStringList(deny.get("monetary_columns"))),
        upperSet(toStringList(deny.get("status_columns"))),
        upperSet(toStringList(deny.get("status_suffixes"))),
        upperSet(toStringList(deny.get("monetary_types"))),
        businessKeys,
        String.valueOf(root.getOrDefault("default_reference_date", "2024-06-15")));
  }

  private static List<String> toStringList(Object value) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return List.of(String.valueOf(value));
  }

  private static Set<String> upperSet(List<String> values) {
    Set<String> out = new LinkedHashSet<>();
    for (String v : values) {
      out.add(v.toUpperCase(Locale.ROOT));
    }
    return out;
  }

  public Set<String> allowTimestamps() {
    return allowTimestamps;
  }

  public Set<String> allowSurrogates() {
    return allowSurrogates;
  }

  public Set<String> denyMonetaryColumns() {
    return denyMonetaryColumns;
  }

  public Set<String> denyStatusColumns() {
    return denyStatusColumns;
  }

  public Set<String> denyMonetaryTypes() {
    return denyMonetaryTypes;
  }

  public Map<String, List<String>> businessKeys() {
    return businessKeys;
  }

  public String defaultReferenceDate() {
    return defaultReferenceDate;
  }

  public boolean isDenied(String columnName) {
    String col = columnName.toUpperCase(Locale.ROOT);
    if (denyMonetaryColumns.contains(col) || denyStatusColumns.contains(col)) {
      return true;
    }
    for (String suffix : statusSuffixes) {
      if (col.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  public boolean isTimestampColumn(String columnName) {
    String col = columnName.toUpperCase(Locale.ROOT);
    if (isDenied(col)) {
      return false;
    }
    if (allowTimestamps.contains(col)) {
      return true;
    }
    for (String suffix : timestampSuffixes) {
      if (col.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  public boolean isSurrogateColumn(String columnName) {
    String col = columnName.toUpperCase(Locale.ROOT);
    if (isDenied(col)) {
      return false;
    }
    if (allowSurrogates.contains(col)) {
      return true;
    }
    for (String suffix : surrogateSuffixes) {
      if (col.endsWith(suffix)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAllowedNormalizeColumn(String columnName) {
    return isTimestampColumn(columnName) || isSurrogateColumn(columnName);
  }
}
