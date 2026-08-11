package com.pcis.premium.support;

import org.testcontainers.DockerClientFactory;

public final class TestEnvironment {

  private TestEnvironment() {}

  public static boolean isDockerAvailable() {
    return DockerClientFactory.instance().isDockerAvailable();
  }
}
