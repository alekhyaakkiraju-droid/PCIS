package com.pcis.sync.health;

import com.pcis.sync.config.SyncAgentProperties;
import com.pcis.sync.watermark.WatermarkRepository;
import com.pcis.sync.watermark.WatermarkState;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("syncHealth")
public class SyncHealthIndicator implements HealthIndicator {

  private final SyncAgentProperties properties;
  private final WatermarkRepository watermarkRepository;
  private final DataSource targetDataSource;
  private final DataSource sourceDataSource;

  public SyncHealthIndicator(
      SyncAgentProperties properties,
      WatermarkRepository watermarkRepository,
      @Qualifier("targetDataSource") DataSource targetDataSource,
      @Qualifier("sourceDataSource") DataSource sourceDataSource) {
    this.properties = properties;
    this.watermarkRepository = watermarkRepository;
    this.targetDataSource = targetDataSource;
    this.sourceDataSource = sourceDataSource;
  }

  @Override
  public Health health() {
    Map<String, Object> details = new LinkedHashMap<>();
    boolean up = true;

    try (var targetConn = targetDataSource.getConnection()) {
      details.put("targetDatabase", targetConn.isValid(2) ? "UP" : "DOWN");
      up &= targetConn.isValid(2);
    } catch (Exception ex) {
      details.put("targetDatabase", "DOWN");
      details.put("targetError", ex.getMessage());
      up = false;
    }

    try (var sourceConn = sourceDataSource.getConnection()) {
      details.put("sourceDatabase", sourceConn.isValid(2) ? "UP" : "DOWN");
      up &= sourceConn.isValid(2);
    } catch (Exception ex) {
      details.put("sourceDatabase", "DOWN");
      details.put("sourceError", ex.getMessage());
      up = false;
    }

    Map<String, Object> domains = new LinkedHashMap<>();
    for (String domainName : properties.getDomains().keySet()) {
      watermarkRepository
          .findByDomain(domainName)
          .ifPresentOrElse(
              state -> domains.put(domainName, domainDetails(state)),
              () -> domains.put(domainName, Map.of("initialized", false)));
    }
    details.put("domains", domains);
    details.put("pollIntervalMs", properties.getPollIntervalMs());
    details.put("chunkSize", properties.getChunkSize());

    Health.Builder builder = up ? Health.up() : Health.down();
    return builder.withDetails(details).build();
  }

  private Map<String, Object> domainDetails(WatermarkState state) {
    Map<String, Object> domain = new LinkedHashMap<>();
    domain.put("watermarkColumn", state.watermarkColumn());
    domain.put("watermarkValue", state.watermarkValue());
    domain.put("lastRunStatus", state.lastRunStatus());
    domain.put("lastRunAt", state.lastRunAt());
    domain.put("rowsExtracted", state.rowsExtracted());
    domain.put("rowsUpserted", state.rowsUpserted());
    return domain;
  }
}
