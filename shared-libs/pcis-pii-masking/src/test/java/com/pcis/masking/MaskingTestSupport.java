package com.pcis.masking;

import com.pcis.classification.ClassificationRegistryParser;
import com.pcis.classification.ClassificationRegistryValidator;
import com.pcis.classification.ClassificationEntry;
import com.pcis.classification.DataClassificationRegistry;
import com.pcis.classification.MaskStrategy;
import com.pcis.masking.mask.FullRedactMasker;
import com.pcis.masking.mask.MaskerRegistry;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public final class MaskingTestSupport {

  private MaskingTestSupport() {}

  public static DataClassificationRegistry registry() {
    var document =
        ClassificationRegistryParser.parse(
            new ClassPathResource("pcis-data-classification-test.yaml"));
    List<ClassificationEntry> entries =
        new ArrayList<>(ClassificationRegistryValidator.validateAndFlatten(document));
    DataClassificationRegistry registry = new DataClassificationRegistry();
    registry.replaceAll(entries);
    return registry;
  }

  public static MaskingService maskingService() {
    return new MaskingService(registry());
  }

  public static MaskingService maskingServiceWithFailingMasker() {
    Map<MaskStrategy, com.pcis.masking.mask.ValueMasker> map = new EnumMap<>(MaskStrategy.class);
    map.put(
        MaskStrategy.LAST_FOUR,
        value -> {
          throw new RuntimeException("boom");
        });
    map.put(MaskStrategy.FULL_REDACT, new FullRedactMasker());
    return new MaskingService(registry(), new MaskerRegistry(map), new PolymorphicMaskResolver());
  }
}
