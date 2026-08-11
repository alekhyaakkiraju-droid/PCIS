package com.pcis.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class BatchPrincipalLiteralsTest {

  private static final java.util.List<String> FORBIDDEN =
      java.util.List.of("BATCHAUD", "BATCHBIL", "BATCHCMM", "BATCHPRM", "BATCHCLM", "BATCHREN");

  @Test
  void noHardCodedBatchActorLiteralsInJavaSources() throws IOException {
    Path repoRoot = Path.of("..", "..").toAbsolutePath().normalize();
    try (Stream<Path> paths = Files.walk(repoRoot.resolve("shared-libs/pcis-config/src/main/java"))) {
      paths
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  String source = Files.readString(path);
                  for (String literal : FORBIDDEN) {
                    assertThat(source)
                        .as("file %s must not contain %s", path.getFileName(), literal)
                        .doesNotContain("\"" + literal + "\"");
                  }
                } catch (IOException e) {
                  throw new IllegalStateException(e);
                }
              });
    }
  }
}
