package com.pcis.sync.sync;

import com.pcis.sync.config.SyncAgentProperties;
import com.pcis.sync.extract.SourceExtractService;
import com.pcis.sync.load.TargetUpsertService;
import com.pcis.sync.metrics.SyncMetrics;
import com.pcis.sync.watermark.WatermarkRepository;
import com.pcis.sync.watermark.WatermarkState;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncAgentService {

  private static final Logger log = LoggerFactory.getLogger(SyncAgentService.class);
  private static final String STATUS_SUCCESS = "SUCCESS";
  private static final String STATUS_FAILED = "FAILED";

  private final SyncAgentProperties properties;
  private final WatermarkRepository watermarkRepository;
  private final SourceExtractService extractService;
  private final TargetUpsertService upsertService;
  private final SyncMetrics syncMetrics;

  public SyncAgentService(
      SyncAgentProperties properties,
      WatermarkRepository watermarkRepository,
      SourceExtractService extractService,
      TargetUpsertService upsertService,
      SyncMetrics syncMetrics) {
    this.properties = properties;
    this.watermarkRepository = watermarkRepository;
    this.extractService = extractService;
    this.upsertService = upsertService;
    this.syncMetrics = syncMetrics;
  }

  @Transactional
  public SyncRunResult syncDomain(String domainName) {
    SyncAgentProperties.DomainProperties domain = properties.getDomains().get(domainName);
    if (domain == null) {
      throw new IllegalArgumentException("Unknown sync domain: " + domainName);
    }

    Instant startedAt = Instant.now();
    long runId = watermarkRepository.insertRunLog(domainName, startedAt);
    log.info(
        "Starting sync for domain={} sourceTable={} watermarkColumn={} chunkSize={}",
        domainName,
        domain.getSourceTable(),
        domain.getWatermarkColumn(),
        properties.getChunkSize());

    watermarkRepository.initialize(
        domainName,
        domain.getSourceTable(),
        domain.getWatermarkColumn(),
        "1970-01-01 00:00:00");

    WatermarkState watermark =
        watermarkRepository
            .findByDomain(domainName)
            .orElseThrow(() -> new IllegalStateException("Watermark not initialized: " + domainName));

    long totalExtracted = 0;
    long totalUpserted = 0;
    String currentWatermark = watermark.watermarkValue();
    String maxWatermark = currentWatermark;

    try {
      while (true) {
        List<Map<String, Object>> batch =
            extractService.extractSinceWatermark(domain, currentWatermark, properties.getChunkSize());
        if (batch.isEmpty()) {
          break;
        }

        int upserted = upsertService.upsertBatch(domain, batch);
        totalExtracted += batch.size();
        totalUpserted += upserted;
        maxWatermark =
            extractService.maxWatermarkFromBatch(batch, domain.getWatermarkColumn(), maxWatermark);
        currentWatermark = maxWatermark;

        if (batch.size() < properties.getChunkSize()) {
          break;
        }
      }

      watermarkRepository.updateAfterRun(
          domainName, maxWatermark, STATUS_SUCCESS, totalExtracted, totalUpserted);
      watermarkRepository.completeRunLog(runId, STATUS_SUCCESS, totalExtracted, totalUpserted, null);
      syncMetrics.recordSuccess(domainName, totalExtracted, totalUpserted);

      log.info(
          "Completed sync for domain={} extracted={} upserted={} watermark={}",
          domainName,
          totalExtracted,
          totalUpserted,
          maxWatermark);

      return new SyncRunResult(domainName, STATUS_SUCCESS, totalExtracted, totalUpserted, maxWatermark);
    } catch (RuntimeException ex) {
      watermarkRepository.updateAfterRun(
          domainName, maxWatermark, STATUS_FAILED, totalExtracted, totalUpserted);
      watermarkRepository.completeRunLog(
          runId, STATUS_FAILED, totalExtracted, totalUpserted, ex.getMessage());
      syncMetrics.recordFailure(domainName);
      log.error("Sync failed for domain={}: {}", domainName, ex.getMessage(), ex);
      throw ex;
    }
  }

  public List<SyncRunResult> syncAllDomains() {
    return properties.getDomains().keySet().stream().map(this::syncDomain).toList();
  }
}
