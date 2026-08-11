package com.pcis.golden;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * In-memory golden artifact matching {@code golden/format-spec.md} v1.0.0.
 */
public final class GoldenArtifact {

  private String formatVersion = "1.0.0";
  private String program;
  private String scenario;
  private String referenceDate;
  private String completionStatus;
  private String displayOutput;
  private Map<String, Object> runLog = new LinkedHashMap<>();
  private List<Map<String, Object>> tables = new ArrayList<>();

  public String getFormatVersion() {
    return formatVersion;
  }

  public void setFormatVersion(String formatVersion) {
    this.formatVersion = formatVersion;
  }

  public String getProgram() {
    return program;
  }

  public void setProgram(String program) {
    this.program = program;
  }

  public String getScenario() {
    return scenario;
  }

  public void setScenario(String scenario) {
    this.scenario = scenario;
  }

  public String getReferenceDate() {
    return referenceDate;
  }

  public void setReferenceDate(String referenceDate) {
    this.referenceDate = referenceDate;
  }

  public String getCompletionStatus() {
    return completionStatus;
  }

  public void setCompletionStatus(String completionStatus) {
    this.completionStatus = completionStatus;
  }

  public String getDisplayOutput() {
    return displayOutput;
  }

  public void setDisplayOutput(String displayOutput) {
    this.displayOutput = displayOutput;
  }

  public Map<String, Object> getRunLog() {
    return runLog;
  }

  public void setRunLog(Map<String, Object> runLog) {
    this.runLog = runLog;
  }

  public List<Map<String, Object>> getTables() {
    return tables;
  }

  public void setTables(List<Map<String, Object>> tables) {
    this.tables = tables;
  }

  /** Canonical map for deterministic JSON serialization (sorted keys). */
  public Map<String, Object> toCanonicalMap() {
    Map<String, Object> root = new TreeMap<>();
    root.put("completionStatus", completionStatus);
    root.put("displayOutput", displayOutput == null ? "" : displayOutput);
    root.put("formatVersion", formatVersion);
    root.put("program", program);
    root.put("referenceDate", referenceDate);
    root.put("runLog", sortMap(runLog));
    root.put("scenario", scenario);
    List<Map<String, Object>> sortedTables = new ArrayList<>();
    for (Map<String, Object> table : tables) {
      sortedTables.add(sortMap(table));
    }
    root.put("tables", sortedTables);
    return root;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> sortMap(Map<String, Object> in) {
    Map<String, Object> out = new TreeMap<>();
    if (in == null) {
      return out;
    }
    for (Map.Entry<String, Object> e : in.entrySet()) {
      Object v = e.getValue();
      if (v instanceof Map<?, ?> nested) {
        out.put(e.getKey(), sortMap((Map<String, Object>) nested));
      } else if (v instanceof List<?> list) {
        List<Object> sortedList = new ArrayList<>();
        for (Object item : list) {
          if (item instanceof Map<?, ?> m) {
            sortedList.add(sortMap((Map<String, Object>) m));
          } else {
            sortedList.add(item);
          }
        }
        out.put(e.getKey(), sortedList);
      } else {
        out.put(e.getKey(), v);
      }
    }
    return out;
  }
}
