package com.pcis.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.outbox")
public class OutboxProperties {

  /** Poll interval for the outbox relay scheduler. */
  private long relayIntervalMs = 5_000L;

  /** Maximum events claimed per relay cycle. */
  private int relayBatchSize = 50;

  /** Maximum publish attempts before transitioning to dead letter. */
  private int relayMaxRetries = 5;

  /** Kafka topic for published domain events. */
  private String kafkaTopic = "audit-events";

  /** Relay operator identity written to {@code UPD_USER}. */
  private String relayUser = "OUTBOXRLY";

  /** Enables the scheduled relay when true. */
  private boolean relayEnabled = true;

  public long getRelayIntervalMs() {
    return relayIntervalMs;
  }

  public void setRelayIntervalMs(long relayIntervalMs) {
    this.relayIntervalMs = relayIntervalMs;
  }

  public int getRelayBatchSize() {
    return relayBatchSize;
  }

  public void setRelayBatchSize(int relayBatchSize) {
    this.relayBatchSize = relayBatchSize;
  }

  public int getRelayMaxRetries() {
    return relayMaxRetries;
  }

  public void setRelayMaxRetries(int relayMaxRetries) {
    this.relayMaxRetries = relayMaxRetries;
  }

  public String getKafkaTopic() {
    return kafkaTopic;
  }

  public void setKafkaTopic(String kafkaTopic) {
    this.kafkaTopic = kafkaTopic;
  }

  public String getRelayUser() {
    return relayUser;
  }

  public void setRelayUser(String relayUser) {
    this.relayUser = relayUser;
  }

  public boolean isRelayEnabled() {
    return relayEnabled;
  }

  public void setRelayEnabled(boolean relayEnabled) {
    this.relayEnabled = relayEnabled;
  }
}
