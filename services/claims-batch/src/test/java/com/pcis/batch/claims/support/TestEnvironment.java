package com.pcis.batch.claims.support;

import org.testcontainers.DockerClientFactory;

public final class TestEnvironment {

  private TestEnvironment() {}

  public static boolean isDockerAvailable() {
    try {
      return DockerClientFactory.instance().isDockerAvailable();
    } catch (RuntimeException ex) {
      return false;
    }
  }
}
