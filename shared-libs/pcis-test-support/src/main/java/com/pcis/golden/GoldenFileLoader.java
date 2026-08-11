package com.pcis.golden;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Loads golden JSON artifacts from {@code golden/outputs/{program}/{scenario}.golden.json}. */
public final class GoldenFileLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final Path goldenRoot;

  public GoldenFileLoader(Path goldenRoot) {
    this.goldenRoot = goldenRoot;
  }

  /**
   * Parses a scenario id of the form {@code program/scenario} (e.g. {@code bil003b/scenario-01}).
   */
  public GoldenArtifact load(String scenarioId) {
    ScenarioRef ref = ScenarioRef.parse(scenarioId);
    Path path = resolveGoldenPath(ref);
    if (!Files.isRegularFile(path)) {
      throw new GoldenFileNotFoundException(path);
    }
    try {
      return MAPPER.readValue(path.toFile(), GoldenArtifact.class);
    } catch (IOException e) {
      throw new GoldenFileNotFoundException(path, e);
    }
  }

  Path resolveGoldenPath(ScenarioRef ref) {
    Path outputs =
        goldenRoot
            .resolve("outputs")
            .resolve(ref.program().toLowerCase(Locale.ROOT))
            .resolve(ref.scenario() + ".golden.json");
    if (Files.isRegularFile(outputs)) {
      return outputs;
    }
    Path direct =
        goldenRoot
            .resolve(ref.program().toLowerCase(Locale.ROOT))
            .resolve(ref.scenario() + ".golden.json");
    if (Files.isRegularFile(direct)) {
      return direct;
    }
    return outputs;
  }

  record ScenarioRef(String program, String scenario) {
    static ScenarioRef parse(String scenarioId) {
      if (scenarioId == null || scenarioId.isBlank()) {
        throw new ConfigurationException("scenarioId must not be blank");
      }
      int slash = scenarioId.indexOf('/');
      if (slash <= 0 || slash >= scenarioId.length() - 1) {
        throw new ConfigurationException(
            "scenarioId must be program/scenario, got: " + scenarioId);
      }
      return new ScenarioRef(
          scenarioId.substring(0, slash).trim(),
          scenarioId.substring(slash + 1).trim());
    }
  }
}
