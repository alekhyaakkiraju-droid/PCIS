package com.pcis.golden;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Enforces that golden capture queries include an explicit {@code ORDER BY}
 * clause so row order is deterministic across runs.
 */
public final class OrderByEnforcer {

  private static final Pattern ORDER_BY =
      Pattern.compile("(?is).*\\border\\s+by\\b.+");

  private OrderByEnforcer() {}

  /**
   * @throws ConfigurationException if the SQL lacks ORDER BY
   */
  public static void requireOrderBy(String sql) {
    if (sql == null || sql.isBlank()) {
      throw new ConfigurationException("Capture SQL must not be blank");
    }
    String trimmed = sql.trim();
    // Strip trailing semicolon for validation.
    if (trimmed.endsWith(";")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
    }
    if (!ORDER_BY.matcher(trimmed).matches()) {
      throw new ConfigurationException(
          "Capture query missing explicit ORDER BY (required for golden determinism): " + sql);
    }
  }

  /**
   * Builds a SELECT * FROM table ORDER BY business-key list.
   */
  public static String selectOrdered(String tableName, List<String> businessKeys) {
    if (businessKeys == null || businessKeys.isEmpty()) {
      throw new ConfigurationException(
          "Business keys required for ordered capture of table " + tableName);
    }
    String order = String.join(", ", businessKeys);
    String sql =
        "SELECT * FROM "
            + tableName.toUpperCase(Locale.ROOT)
            + " ORDER BY "
            + order;
    requireOrderBy(sql);
    return sql;
  }

  public static boolean hasOrderBy(String sql) {
    try {
      requireOrderBy(sql);
      return true;
    } catch (ConfigurationException e) {
      return false;
    }
  }
}
