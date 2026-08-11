package com.pcis.masking.logback;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.pcis.masking.MaskingTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClassificationLogMaskingConverterTest {

  private ClassificationLogMaskingConverter converter;
  private Logger logger;

  @BeforeEach
  void setUp() {
    LogbackMaskingBridge.initialize(
        MaskingTestSupport.maskingService(), MaskingTestSupport.registry());
    converter = new ClassificationLogMaskingConverter();
    LoggerContext context = new LoggerContext();
    converter.setContext(context);
    logger = context.getLogger("test");
  }

  @AfterEach
  void tearDown() {
    LogbackMaskingBridge.reset();
  }

  @Test
  void masksTaxIdUsingRegistryLastFourStrategy() {
    assertThat(ClassificationLogMaskingConverter.maskPii("tax=123-45-6789"))
        .isEqualTo("tax=6789")
        .doesNotContain("123-45-6789");
  }

  @Test
  void masksPlainNineDigitTaxId() {
    assertThat(ClassificationLogMaskingConverter.maskPii("tax=987654321"))
        .isEqualTo("tax=4321")
        .doesNotContain("987654321");
  }

  @ParameterizedTest
  @CsvSource({
    "555-123-4567,4567",
    "(415) 555-0100,0100",
    "4155550100,0100",
    "+1-202-555-0182,0182"
  })
  void masksPhoneVariants(String phone, String expectedLastFour) {
    String masked = ClassificationLogMaskingConverter.maskPii("call " + phone);
    assertThat(masked).contains(expectedLastFour);
    assertThat(masked).doesNotContain(phone.replaceAll("\\D", ""));
  }

  @Test
  void masksEmailUsingDomainOnlyStrategy() {
    assertThat(ClassificationLogMaskingConverter.maskPii("mail jane.doe@example.com"))
        .isEqualTo("mail example.com");
  }

  @Test
  void masksDateOfBirthToYearOnly() {
    assertThat(ClassificationLogMaskingConverter.maskPii("dob=1985-03-15"))
        .isEqualTo("dob=1985");
  }

  @Test
  void passthroughWhenNoPiiPresent() {
    String input = "No PII in this operational heartbeat message";
    assertThat(ClassificationLogMaskingConverter.maskPii(input)).isEqualTo(input);
  }

  @Test
  void usesFallbackRegistryBeforeSpringInitialization() {
    LogbackMaskingBridge.reset();
    assertThat(ClassificationLogMaskingConverter.maskPii("tax=123-45-6789"))
        .isEqualTo("tax=6789");
  }

  private LoggingEvent event(String message) {
    return new LoggingEvent("fqcn", logger, Level.INFO, message, null, null);
  }
}
