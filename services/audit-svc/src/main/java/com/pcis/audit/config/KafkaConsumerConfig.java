package com.pcis.audit.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
@ConditionalOnProperty(prefix = "pcis.audit.kafka", name = "consumer-enabled", havingValue = "true")
public class KafkaConsumerConfig {}
