package com.pcis.billing.support;

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
