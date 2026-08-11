package com.pcis.policy.support;

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
