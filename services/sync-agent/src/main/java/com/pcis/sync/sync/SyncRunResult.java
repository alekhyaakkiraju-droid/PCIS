package com.pcis.sync.sync;

public record SyncRunResult(
    String domainName, String status, long rowsExtracted, long rowsUpserted, String watermarkValue) {}
