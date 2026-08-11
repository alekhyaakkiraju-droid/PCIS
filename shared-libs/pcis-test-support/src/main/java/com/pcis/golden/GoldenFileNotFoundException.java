package com.pcis.golden;

import java.nio.file.Path;

/** Thrown when a golden file cannot be loaded from the expected path. */
public final class GoldenFileNotFoundException extends RuntimeException {

  private final Path expectedPath;

  public GoldenFileNotFoundException(Path expectedPath, Throwable cause) {
    super("Golden file not found: " + expectedPath, cause);
    this.expectedPath = expectedPath;
  }

  public GoldenFileNotFoundException(Path expectedPath) {
    super("Golden file not found: " + expectedPath);
    this.expectedPath = expectedPath;
  }

  public Path getExpectedPath() {
    return expectedPath;
  }
}
