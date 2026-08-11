package com.pcis.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PiiMaskingConverterTest {

  private PiiMaskingConverter converter;
  private Logger logger;

  @BeforeEach
  void setUp() {
    converter = new PiiMaskingConverter();
    LoggerContext context = new LoggerContext();
    converter.setContext(context);
    logger = context.getLogger("test");
  }

  @Test
  void masksSingleSsn() {
    assertThat(PiiMaskingConverter.maskPii("ssn=123-45-6789")).isEqualTo("ssn=***-**-****");
  }

  @Test
  void masksMultipleSsns() {
    String input = "a=111-22-3333 b=444-55-6666";
    assertThat(PiiMaskingConverter.maskPii(input)).isEqualTo("a=***-**-**** b=***-**-****");
  }

  @Test
  void masksPlainNineDigitTaxId() {
    assertThat(PiiMaskingConverter.maskPii("tax=987654321")).contains("***-**-****");
    assertThat(PiiMaskingConverter.maskPii("tax=987654321")).doesNotContain("987654321");
  }

  @ParameterizedTest
  @CsvSource({
    "555-123-4567,****-4567",
    "(415) 555-0100,****-0100",
    "4155550100,****-0100",
    "+1-202-555-0182,****-0182"
  })
  void masksPhoneVariants(String phone, String expectedFragment) {
    String masked = PiiMaskingConverter.maskPii("call " + phone);
    assertThat(masked).contains(expectedFragment);
    String digits = phone.replaceAll("\\D", "");
    assertThat(masked).doesNotContain(digits);
  }

  @Test
  void masksEmailKeepingDomainAndTwoCharPrefix() {
    assertThat(PiiMaskingConverter.maskPii("mail jane.doe@example.com"))
        .isEqualTo("mail ja***@example.com");
    assertThat(PiiMaskingConverter.maskPii("ab@x.co")).isEqualTo("ab***@x.co");
  }

  @Test
  void masksMixedPiiInOneMessage() {
    String masked =
        PiiMaskingConverter.maskPii(
            "SSN 123-45-6789 PHONE 555-123-4567 EMAIL bob.smith@acme.org");
    assertThat(masked)
        .contains("***-**-****")
        .contains("****-4567")
        .contains("bo***@acme.org")
        .doesNotContain("123-45-6789")
        .doesNotContain("555-123-4567")
        .doesNotContain("bob.smith@acme.org");
  }

  @Test
  void masksPiiInsideExceptionMessage() {
    LoggingEvent event =
        new LoggingEvent(
            "fqcn",
            logger,
            Level.ERROR,
            "failed for 222-33-4444 victim@mail.test",
            new IllegalStateException("ssn 999-88-7777"),
            null);
    String masked = converter.convert(event);
    assertThat(masked).doesNotContain("222-33-4444").doesNotContain("victim@mail.test");
    assertThat(PiiMaskingConverter.maskPii(event.getThrowableProxy().getMessage()))
        .isEqualTo("ssn ***-**-****");
  }

  @Test
  void passthroughWhenNoPiiPresent() {
    String input = "No PII in this operational heartbeat message";
    assertThat(PiiMaskingConverter.maskPii(input)).isEqualTo(input);
    assertThat(converter.convert(event(input))).isEqualTo(input);
  }

  @Test
  void convertHandlesNullEventAndNullMessage() {
    assertThat(converter.convert(null)).isEmpty();
    LoggingEvent event = new LoggingEvent("fqcn", logger, Level.INFO, null, null, null);
    assertThat(converter.convert(event)).isEmpty();
  }

  @Test
  void fixtureFileGoldenOutputs() throws IOException {
    List<String> inputs = readFixture("fixtures/sample-pii-messages.txt");
    List<String> expected = readFixture("fixtures/expected-masked-messages.txt");
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
