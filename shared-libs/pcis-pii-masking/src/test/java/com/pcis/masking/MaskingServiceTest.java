package com.pcis.masking;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcis.classification.MaskStrategy;
import com.pcis.masking.mask.DateYearOnlyMasker;
import com.pcis.masking.mask.EmailDomainOnlyMasker;
import com.pcis.masking.mask.FullRedactMasker;
import com.pcis.masking.mask.LastFourMasker;
import com.pcis.masking.mask.NoneMasker;
import com.pcis.masking.mask.PhoneLastFourMasker;
import java.io.IOException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

class MaskingServiceTest {

  private MaskingService maskingService;

  @BeforeEach
  void setUp() {
    maskingService = MaskingTestSupport.maskingService();
  }

  @Test
  void maskByClassificationUsesDedicatedMaskers() {
    assertThat(maskingService.maskByClassification("visible", MaskStrategy.NONE))
        .isEqualTo("visible");
    assertThat(maskingService.maskByClassification("12-3456789", MaskStrategy.LAST_FOUR))
        .isEqualTo("6789");
    assertThat(maskingService.maskByClassification("a@b.com", MaskStrategy.EMAIL_DOMAIN_ONLY))
        .isEqualTo("b.com");
    assertThat(maskingService.maskByClassification("5551234567", MaskStrategy.PHONE_LAST_FOUR))
        .isEqualTo("4567");
    assertThat(maskingService.maskByClassification("1990-01-02", MaskStrategy.DATE_YEAR_ONLY))
        .isEqualTo("1990");
    assertThat(maskingService.maskByClassification("secret", MaskStrategy.FULL_REDACT))
        .isEqualTo(MaskingConstants.FULL_REDACT_TOKEN);
  }

  @Test
  void maskByClassificationHandlesNull() {
    assertThat(maskingService.maskByClassification(null, MaskStrategy.LAST_FOUR)).isNull();
  }

  @Test
  void maskByClassificationNeverThrows() {
    MaskingService failingService = MaskingTestSupport.maskingServiceWithFailingMasker();
    assertThat(failingService.maskByClassification("value", MaskStrategy.LAST_FOUR))
        .isEqualTo(MaskingConstants.FULL_REDACT_TOKEN);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtureMaskStrategies")
  void maskByClassificationMatchesFixtures(
      String description, MaskStrategy strategy, String input, String expected) {
    assertThat(maskingService.maskByClassification(input, strategy)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixtureRegistryLookups")
  void maskResolvesRegistryEntries(
      String description, String entity, String column, String input, String expected) {
    assertThat(maskingService.mask(entity, column, input)).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("fixturePolymorphicContacts")
  void maskResolvesPolymorphicDiscriminators(
      String description,
      String entity,
      String column,
      String discriminatorValue,
      String input,
      String expected) {
    assertThat(maskingService.mask(entity, column, input, discriminatorValue)).isEqualTo(expected);
  }

  @Test
  void individualMaskersAreIdempotent() {
    assertThat(new LastFourMasker().mask("6789")).isEqualTo("6789");
    assertThat(new EmailDomainOnlyMasker().mask("example.com")).isEqualTo("example.com");
    assertThat(new PhoneLastFourMasker().mask("4567")).isEqualTo("4567");
    assertThat(new DateYearOnlyMasker().mask("1985")).isEqualTo("1985");
    assertThat(new FullRedactMasker().mask(MaskingConstants.FULL_REDACT_TOKEN))
        .isEqualTo(MaskingConstants.FULL_REDACT_TOKEN);
    assertThat(new NoneMasker().mask("plain")).isEqualTo("plain");
  }

  private static Stream<Arguments> fixtureMaskStrategies() throws IOException {
    return readFixtureArray("maskStrategies")
        .map(
            node ->
                Arguments.of(
                    node.path("description").asText(node.path("strategy").asText()),
                    MaskStrategy.valueOf(node.get("strategy").asText()),
                    nullableText(node.get("input")),
                    nullableText(node.get("expected"))));
  }

  private static Stream<Arguments> fixtureRegistryLookups() throws IOException {
    return readFixtureArray("registryLookups")
        .map(
            node ->
                Arguments.of(
                    node.path("description").asText(node.path("column").asText()),
                    node.get("entity").asText(),
                    node.get("column").asText(),
                    nullableText(node.get("input")),
                    node.get("expected").asText()));
  }

  private static Stream<Arguments> fixturePolymorphicContacts() throws IOException {
    return readFixtureArray("polymorphicContacts")
        .map(
            node ->
                Arguments.of(
                    node.path("description").asText(node.path("discriminatorValue").asText()),
                    node.get("entity").asText(),
                    node.get("column").asText(),
                    node.get("discriminatorValue").asText(),
                    nullableText(node.get("input")),
                    node.get("expected").asText()));
  }

  private static Stream<JsonNode> readFixtureArray(String arrayName) throws IOException {
    JsonNode root =
        new ObjectMapper()
            .readTree(new ClassPathResource("pii-fixtures.json").getInputStream());
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(root.get(arrayName).elements(), Spliterator.ORDERED),
        false);
  }

  private static String nullableText(JsonNode node) {
    return node == null || node.isNull() ? null : node.asText();
  }
}
