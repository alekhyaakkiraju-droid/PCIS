package com.pcis.golden;

/** JUnit-friendly assertion failure carrying the full structured diff. */
public final class GoldenComparisonFailure extends AssertionError {

  private final GoldenDiff diff;

  public GoldenComparisonFailure(GoldenDiff diff) {
    super(formatMessage(diff));
    this.diff = diff;
  }

  public GoldenDiff getDiff() {
    return diff;
  }

  private static String formatMessage(GoldenDiff diff) {
    StringBuilder sb = new StringBuilder();
    sb.append("Golden comparison failed for scenario ")
        .append(diff.getScenarioId())
        .append(": ")
        .append(diff.getTotalDiffCount())
        .append(" difference(s)");
    if (diff.isTruncated()) {
      sb.append(" (showing first ").append(diff.getEntries().size()).append(")");
    }
    sb.append('\n');
    sb.append(GoldenDiffTextWriter.render(diff));
    return sb.toString();
  }
}
