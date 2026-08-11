package com.pcis.batch.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BatchConfigurationExceptionTest {

  @Test
  void exitCodeIsFive() {
    BatchConfigurationException ex = new BatchConfigurationException("auth failed");
    assertThat(ex.getExitCode()).isEqualTo(5);
    assertThat(BatchConfigurationException.EXIT_CODE).isEqualTo(5);
  }
}
