package com.pcis.masking;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.classification.DataClassificationRegistry;
import com.pcis.masking.jackson.PcisJacksonMaskingModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ObjectMapper.class)
@ConditionalOnBean(DataClassificationRegistry.class)
@ConditionalOnProperty(prefix = "pcis.pii-masking", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PiiMaskingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  MaskingService maskingService(DataClassificationRegistry registry) {
    return new MaskingService(registry);
  }

  @Bean
  @ConditionalOnMissingBean(name = "pcisJacksonMaskingModule")
  Module pcisJacksonMaskingModule(MaskingService maskingService) {
    return new PcisJacksonMaskingModule(maskingService);
  }
}
