package com.pcis.golden;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Structured diff report for golden vs actual comparison. */
public final class GoldenDiff {

  private final String scenarioId;
  private final boolean match;
  private final int totalDiffCount;
  private final boolean truncated;
  private final List<GoldenDiffEntry> entries;
  private final Map<DiffCategory, Integer> countsByCategory;

  public GoldenDiff(
      String scenarioId,
      boolean match,
      int totalDiffCount,
      boolean truncated,
      List<GoldenDiffEntry> entries,
      Map<DiffCategory, Integer> countsByCategory) {
    this.scenarioId = scenarioId;
    this.match = match;
    this.totalDiffCount = totalDiffCount;
    this.truncated = truncated;
    this.entries = List.copyOf(entries);
    this.countsByCategory = Map.copyOf(countsByCategory);
  }

  public String getScenarioId() {
    return scenarioId;
  }

  public boolean isMatch() {
    return match;
  }

  public int getTotalDiffCount() {
    return totalDiffCount;
  }

  public boolean isTruncated() {
    return truncated;
  }

  public List<GoldenDiffEntry> getEntries() {
    return entries;
  }

  public Map<DiffCategory, Integer> getCountsByCategory() {
    return countsByCategory;
  }

  static Builder builder(String scenarioId) {
    return new Builder(scenarioId);
  }

  static final class Builder {
    private final String scenarioId;
    private final List<GoldenDiffEntry> allEntries = new ArrayList<>();
    private final Map<DiffCategory, Integer> counts = new EnumMap<>(DiffCategory.class);

    Builder(String scenarioId) {
      this.scenarioId = scenarioId;
    }

    void add(GoldenDiffEntry entry) {
      allEntries.add(entry);
      counts.merge(entry.getCategory(), 1, Integer::sum);
    }

    GoldenDiff build(int maxEntries) {
      int total = allEntries.size();
      boolean truncated = total > maxEntries;
      List<GoldenDiffEntry> visible =
          truncated ? allEntries.subList(0, maxEntries) : List.copyOf(allEntries);
      return new GoldenDiff(
          scenarioId, total == 0, total, truncated, visible, Collections.unmodifiableMap(counts));
    }
  }
}
