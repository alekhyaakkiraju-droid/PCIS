package com.pcis.classification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/** Parses pcis-data-classification.yaml into a validated document. */
public final class ClassificationRegistryParser {

  private static final ObjectMapper YAML =
      new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

  private ClassificationRegistryParser() {}

  public static ClassificationRegistryDocument parse(Resource resource) {
    try (InputStream in = resource.getInputStream()) {
      return YAML.readValue(in, ClassificationRegistryDocument.class);
    } catch (IOException ex) {
      throw new ClassificationRegistryException(
          "Failed to read classification registry from " + resource, ex);
    }
  }

  public static ClassificationRegistryDocument parse(Path path) {
    try {
      return YAML.readValue(Files.readString(path), ClassificationRegistryDocument.class);
    } catch (IOException ex) {
      throw new ClassificationRegistryException(
          "Failed to read classification registry from " + path, ex);
    }
  }

  public static ClassificationRegistryDocument parseYaml(String yaml) {
    try {
      return YAML.readValue(yaml, ClassificationRegistryDocument.class);
    } catch (IOException ex) {
      throw new ClassificationRegistryException("Failed to parse classification registry YAML", ex);
    }
  }

  public static String readRaw(Resource resource) {
    try (InputStream in = resource.getInputStream()) {
      return StreamUtils.copyToString(in, java.nio.charset.StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new ClassificationRegistryException(
          "Failed to read classification registry from " + resource, ex);
    }
  }
}
