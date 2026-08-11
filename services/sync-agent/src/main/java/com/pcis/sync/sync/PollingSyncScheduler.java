package com.pcis.sync.sync;

import com.pcis.sync.config.SyncAgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "pcis.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PollingSyncScheduler {

  private static final Logger log = LoggerFactory.getLogger(PollingSyncScheduler.class);

  private final SyncAgentProperties properties;
  private final SyncAgentService syncAgentService;

  public PollingSyncScheduler(SyncAgentProperties properties, SyncAgentService syncAgentService) {
    this.properties = properties;
    this.syncAgentService = syncAgentService;
  }

  @Scheduled(fixedDelayString = "${pcis.sync.poll-interval-ms:60000}")
  public void pollAllDomains() {
    if (properties.getDomains().isEmpty()) {
      return;
    }
    log.debug("Polling sync cycle starting for {} domain(s)", properties.getDomains().size());
    for (String domainName : properties.getDomains().keySet()) {
      try {
        syncAgentService.syncDomain(domainName);
      } catch (RuntimeException ex) {
        log.warn("Scheduled sync failed for domain={}: {}", domainName, ex.getMessage());
      }
    }
  }
}
