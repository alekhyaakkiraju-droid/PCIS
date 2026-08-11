package com.pcis.batch.common;

public record BatchRunLogCounters(
    int recSelected, int recUpdated, int recErrors, Integer recDelinquent) {

  public static BatchRunLogCounters of(int recSelected, int recUpdated, int recErrors) {
    return new BatchRunLogCounters(recSelected, recUpdated, recErrors, null);
  }

  public static BatchRunLogCounters withDelinquent(
      int recSelected, int recUpdated, int recErrors, int recDelinquent) {
    return new BatchRunLogCounters(recSelected, recUpdated, recErrors, recDelinquent);
  }
}
