package com.pcis.customer.support;

import com.pcis.classification.ClassificationRegistryParser;
import com.pcis.classification.InMemoryDataClassificationRegistry;
import com.pcis.masking.MaskingService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

@TestConfiguration
public class TestMaskingConfig {

  @Bean
  MaskingService maskingService() {
    var document =
        ClassificationRegistryParser.parse(new ClassPathResource("pcis-data-classification-test.yaml"));
    var registry = InMemoryDataClassificationRegistry.fromDocument(document).registry();
    return new MaskingService(registry);
  }
}
