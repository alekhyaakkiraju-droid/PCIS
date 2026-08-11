package com.pcis.reporting.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ReadOnlyViolationLogger {
  private static final Logger log = LoggerFactory.getLogger(ReadOnlyViolationLogger.class);

  public void logViolation(String actor, String resource, String operation) {
    log.warn("read_only_violation actor={} resource={} operation={}", actor, resource, operation);
  }
}
