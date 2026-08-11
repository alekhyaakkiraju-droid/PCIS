package com.pcis.classification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DataClassificationLoaderDriftTest {

  @Test
  void detectDriftFailsWhenRegistryMissingSchemaColumn() {
    var document =
        ClassificationRegistryParser.parse(
            new ClassPathResource("pcis-data-classification-unclassified.yaml"));
    List<ClassificationEntry> entries = ClassificationRegistryValidator.validateAndFlatten(document);

    var loader =
        new DataClassificationLoader(
            null, new DataClassificationProperties(), new DataClassificationRegistry(), null);

    assertThatThrownBy(
            () ->
                loader.detectDrift(
                    entries, Map.of("CUSTOMER_T", Set.of("CUST_ID", "TAX_ID"))))
        .isInstanceOf(ClassificationDriftException.class)
        .hasMessageContaining("Column drift");
  }
}
