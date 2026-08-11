package com.pcis.sync.support;

public final class TestEnvironment {

  private TestEnvironment() {}

  public static boolean isDockerAvailable() {
    try {
      org.testcontainers.DockerClientFactory.instance().client();
      return true;
    } catch (RuntimeException ex) {
      return false;
    }
  }
}
