package com.pcis.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.pcis.masking.logback.LogbackMaskingBridge;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PiiMaskingConverterTest {

  private PiiMaskingConverter converter;
  private Logger logger;

  @BeforeEach
  void setUp() {
    LogbackMaskingBridge.reset();
    converter = new PiiMaskingConverter();
    LoggerContext context = new LoggerContext();
    converter.setContext(context);
    logger = context.getLogger("test");
  }

  @AfterEach
  void tearDown() {
    LogbackMaskingBridge.reset();
  }

  @Test
  void masksTaxIdEmailAndPhoneUsingRegistryStrategies() {
    String masked =
        PiiMaskingConverter.maskPii(
            "SSN 123-45-6789 PHONE 555-123-4567 EMAIL bob.smith@acme.org");
    assertThat(masked)
        .contains("6789", "4567", "acme.org")
        .doesNotContain("123-45-6789", "555-123-4567", "bob.smith@acme.org");
  }

  @Test
  void passthroughWhenNoPiiPresent() {
    String input = "No PII in this operational heartbeat message";
    assertThat(PiiMaskingConverter.maskPii(input)).isEqualTo(input);
  }

  @Test
  void fixtureFileGoldenOutputs() throws IOException {
    List<String> inputs = readFixture("fixtures/sample-pii-messages.txt");
    List<String> expected = readFixture("fixtures/expected-masked-messages-registry.txt");
    assertThat(inputs).hasSameSizeAs(expected);
    for (int i = 0; i < inputs.size(); i++) {
      assertThat(PiiMaskingConverter.maskPii(inputs.get(i)))
          .as("line %s", i + 1)
          .isEqualTo(expected.get(i));
    }
  }

  private static List<String> readFixture(String classpathLocation) throws IOException {
    try (var in = PiiMaskingConverterTest.class.getClassLoader().getResourceAsStream(classpathLocation)) {
      assertThat(in).as("missing classpath resource %s", classpathLocation).isNotNull();
      return new String(in.readAllBytes()).lines().toList();
    }
  }

  private LoggingEvent event(String message) {
    return new LoggingEvent("fqcn", logger, Level.INFO, message, null, null);
  }
}
