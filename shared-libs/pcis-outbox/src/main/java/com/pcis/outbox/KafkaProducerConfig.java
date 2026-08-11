package com.pcis.outbox;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(
    prefix = "pcis.outbox",
    name = "kafka-autoconfigure",
    havingValue = "true",
    matchIfMissing = true)
public class KafkaProducerConfig {

  @Bean
  @ConditionalOnMissingBean(KafkaTemplate.class)
  ProducerFactory<String, String> outboxProducerFactory(Environment environment) {
    Map<String, Object> props = new HashMap<>();
    props.put(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092"));
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    return new DefaultKafkaProducerFactory<>(props);
  }

  @Bean
  @ConditionalOnMissingBean(KafkaTemplate.class)
  KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> outboxProducerFactory) {
    return new KafkaTemplate<>(outboxProducerFactory);
  }
}
