package com.pcis.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pcis.notification.kafka")
public record NotificationKafkaProperties(
    boolean consumerEnabled, String consumerGroup, String notificationEventPrefix) {
  public NotificationKafkaProperties {
    if (consumerGroup == null || consumerGroup.isBlank()) {
      consumerGroup = "reporting-svc-notification";
    }
    if (notificationEventPrefix == null || notificationEventPrefix.isBlank()) {
      notificationEventPrefix = "Notification";
    }
  }
}
