package com.pcis.classification;

import java.util.Collection;
import java.util.List;

/** Test double that wraps an in-memory registry without Spring or database dependencies. */
public final class InMemoryDataClassificationRegistry {

  private final DataClassificationRegistry delegate = new DataClassificationRegistry();

  private InMemoryDataClassificationRegistry(Collection<ClassificationEntry> entries) {
    delegate.replaceAll(entries);
  }

  public static InMemoryDataClassificationRegistry fromDocument(
      ClassificationRegistryDocument document) {
    List<ClassificationEntry> entries = ClassificationRegistryValidator.validateAndFlatten(document);
    return new InMemoryDataClassificationRegistry(entries);
  }

  public static InMemoryDataClassificationRegistry fromYaml(String yaml) {
    return fromDocument(ClassificationRegistryParser.parseYaml(yaml));
  }

  public DataClassificationRegistry registry() {
    return delegate;
  }
}
