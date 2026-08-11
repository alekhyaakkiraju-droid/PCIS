package com.pcis.customer.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PiiMaskingConverterTest {

  @ParameterizedTest(name = "SSN \"{0}\" masked to \"{1}\"")
  @CsvSource({
    "123-45-6789, ***-**-6789",
    "SSN: 987-65-4321 redacted, SSN: ***-**-4321 redacted",
    "000-00-0000 test, ***-**-0000 test",
  })
  void ssnIsMaskedPreservingLastFour(String input, String expected) {
    assertThat(PiiMaskingConverter.mask(input)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "Phone \"{0}\" is masked")
  @CsvSource({
    "8005551234",
    "8005551234567",
    "12125559876",
  })
  void phoneNumberIsMasked(String phone) {
    String result = PiiMaskingConverter.mask("Call " + phone + " now");
    assertThat(result).contains("[PHONE]");
    assertThat(result).doesNotContain(phone);
  }

  @ParameterizedTest(name = "Email \"{0}\" is masked")
  @CsvSource({
    "john.doe@example.com",
    "user+tag@sub.domain.org",
    "customer@pcis.example",
  })
  void emailAddressIsMasked(String email) {
    String result = PiiMaskingConverter.mask("Contact: " + email);
    assertThat(result).contains("***@***.***");
    assertThat(result).doesNotContain(email);
  }

  @ParameterizedTest(name = "Tax ID \"{0}\" is masked")
  @CsvSource({
    "12-3456789",
    "EIN: 98-7654321 on file",
  })
  void taxIdIsMasked(String input) {
    String result = PiiMaskingConverter.mask(input);
    assertThat(result).doesNotContain("12-3456789");
    assertThat(result).doesNotContain("98-7654321");
  }

  @Test
  void nonPiiStringPassesThroughUnchanged() {
    String input = "Customer record updated successfully for account ACT-001234";
    assertThat(PiiMaskingConverter.mask(input)).isEqualTo(input);
  }

  @Test
  void nullReturnsNull() {
    assertThat(PiiMaskingConverter.mask(null)).isNull();
  }

  @Test
  void emptyStringReturnsEmpty() {
    assertThat(PiiMaskingConverter.mask("")).isEmpty();
  }

  @Test
  void multiplePatternsMaskedInSingleMessage() {
    String input = "Agent jane@example.com created customer SSN 123-45-6789 phone 8005551234";
    String result = PiiMaskingConverter.mask(input);
    assertThat(result).contains("***@***.***");
    assertThat(result).contains("***-**-6789");
    assertThat(result).contains("[PHONE]");
    assertThat(result).doesNotContain("jane@example.com");
    assertThat(result).doesNotContain("123-45-6789");
    assertThat(result).doesNotContain("8005551234");
  }
}
