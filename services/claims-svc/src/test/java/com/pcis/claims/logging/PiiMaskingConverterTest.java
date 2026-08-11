package com.pcis.claims.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PiiMaskingConverterTest {

  @ParameterizedTest(name = "SSN masking: {0} → {1}")
  @CsvSource({
      "'SSN 123-45-6789 found',      'SSN ***-**-6789 found'",
      "'Claimant SSN: 999-88-7777',  'Claimant SSN: ***-**-7777'",
  })
  void masksSSNPreservingLastFour(String input, String expected) {
    assertThat(PiiMaskingConverter.mask(input)).isEqualTo(expected);
  }

  @Test
  void masksTenDigitPhone() {
    assertThat(PiiMaskingConverter.mask("Call 8005551234 now"))
        .isEqualTo("Call [PHONE] now");
  }

  @Test
  void masksEmailAddress() {
    assertThat(PiiMaskingConverter.mask("Contact john.doe@example.com for details"))
        .isEqualTo("Contact ***@***.*** for details");
  }

  @ParameterizedTest(name = "Tax ID masking: {0} → {1}")
  @CsvSource({
      "'FEIN 12-3456789', 'FEIN **-****789'",
      "'TaxID: 99-8765432', 'TaxID: **-****432'",
  })
  void masksTaxIdPreservingLastThree(String input, String expected) {
    assertThat(PiiMaskingConverter.mask(input)).isEqualTo(expected);
  }

  @Test
  void passesNonPiiTextUnchanged() {
    String input = "Processing claim CLM-000001 reserve_type=PRO";
    assertThat(PiiMaskingConverter.mask(input)).isEqualTo(input);
  }

  @Test
  void handlesNullInput() {
    assertThat(PiiMaskingConverter.mask(null)).isNull();
  }

  @Test
  void handlesEmptyString() {
    assertThat(PiiMaskingConverter.mask("")).isEqualTo("");
  }

  @Test
  void masksMultiplePiiPatternsInOneLine() {
    String input = "Claimant 123-45-6789 email john@test.org phone 5555551234";
    String result = PiiMaskingConverter.mask(input);
    assertThat(result)
        .contains("***-**-6789")
        .contains("***@***.***")
        .contains("[PHONE]")
        .doesNotContain("123-45-6789")
        .doesNotContain("john@test.org")
        .doesNotContain("5555551234");
  }
}
