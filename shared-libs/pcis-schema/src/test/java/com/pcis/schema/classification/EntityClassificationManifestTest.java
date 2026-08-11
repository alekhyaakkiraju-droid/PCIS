package com.pcis.schema.classification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityClassificationManifestTest {

    private static final String VALID_ENTRY = """
            manifest_version: 1
            woref: WO-154
            table_count: 1
            entries:
              - table_name: CUSTOMER_T
                classification_tier: RESTRICTED
                retention_days: 365
                pii_columns:
                  - TAX_ID
                description: Customer master
            """;

    @Test
    void parsesValidManifestFromRepositoryFile() throws IOException {
        EntityClassificationManifest manifest =
                EntityClassificationManifest.load(EntityClassificationManifest.resolveManifestPath());

        assertEquals(1, manifest.manifestVersion());
        assertEquals(57, manifest.entries().size());
        assertEquals(
                ClassificationTier.RESTRICTED,
                manifest.entriesByTableName().get("CUSTOMER_T").classificationTier());
        assertEquals(
                ClassificationTier.CONFIDENTIAL,
                manifest.entriesByTableName().get("CLAIM_PAYMENT_T").classificationTier());
        assertEquals(
                ClassificationTier.PUBLIC,
                manifest.entriesByTableName().get("COVERAGE_TYPE_T").classificationTier());
    }

    @Test
    void rejectsMissingRequiredFields() {
        String yaml = """
                manifest_version: 1
                entries:
                  - table_name: CUSTOMER_T
                    classification_tier: RESTRICTED
                """;
        EntityClassificationManifest.ManifestParseException ex = assertThrows(
                EntityClassificationManifest.ManifestParseException.class,
                () -> parse(yaml));
        assertTrue(ex.getMessage().contains("retention_days"));
    }

    @Test
    void rejectsInvalidTierEnum() {
        String yaml = """
                manifest_version: 1
                entries:
                  - table_name: CUSTOMER_T
                    classification_tier: SECRET
                    retention_days: 365
                    pii_columns: []
                    description: test
                """;
        EntityClassificationManifest.ManifestParseException ex = assertThrows(
                EntityClassificationManifest.ManifestParseException.class,
                () -> parse(yaml));
        assertTrue(ex.getMessage().contains("classification_tier"));
    }

    @Test
    void rejectsRestrictedRetentionBelowMinimum() {
        String yaml = """
                manifest_version: 1
                entries:
                  - table_name: CUSTOMER_T
                    classification_tier: RESTRICTED
                    retention_days: 90
                    pii_columns: []
                    description: test
                """;
        EntityClassificationManifest.ManifestParseException ex = assertThrows(
                EntityClassificationManifest.ManifestParseException.class,
                () -> parse(yaml));
        assertTrue(ex.getMessage().contains("retention_days >= 365"));
    }

    @Test
    void rejectsConfidentialRetentionBelowMinimum() {
        String yaml = """
                manifest_version: 1
                entries:
                  - table_name: INVOICE_T
                    classification_tier: CONFIDENTIAL
                    retention_days: 180
                    pii_columns: []
                    description: test
                """;
        EntityClassificationManifest.ManifestParseException ex = assertThrows(
                EntityClassificationManifest.ManifestParseException.class,
                () -> parse(yaml));
        assertTrue(ex.getMessage().contains("retention_days >= 365"));
    }

    @Test
    void validateCompletenessDetectsMissingManifestEntry(@TempDir Path tempDir) throws IOException {
        Path manifestFile = tempDir.resolve("entity-classification.yaml");
        Files.writeString(manifestFile, VALID_ENTRY, StandardCharsets.UTF_8);

        EntityClassificationManifest manifest = EntityClassificationManifest.load(manifestFile);
        var errors = manifest.validateCompleteness(Set.of("CUSTOMER_T", "CLAIM_T"));

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().contains("Unclassified schema table: CLAIM_T"));
    }

    @Test
    void validateCompletenessDetectsExtraManifestEntry(@TempDir Path tempDir) throws IOException {
        String yaml = """
                manifest_version: 1
                entries:
                  - table_name: CUSTOMER_T
                    classification_tier: RESTRICTED
                    retention_days: 365
                    pii_columns: []
                    description: Customer master
                  - table_name: CLAIM_T
                    classification_tier: INTERNAL
                    retention_days: 180
                    pii_columns: []
                    description: Claim header
                """;
        Path manifestFile = tempDir.resolve("entity-classification.yaml");
        Files.writeString(manifestFile, yaml, StandardCharsets.UTF_8);

        EntityClassificationManifest manifest = EntityClassificationManifest.load(manifestFile);
        var errors = manifest.validateCompleteness(Set.of("CUSTOMER_T"));

        assertEquals(1, errors.size());
        assertTrue(errors.getFirst().contains("Manifest entry not present in schema: CLAIM_T"));
    }

    @Test
    void normalizesOutboxEventsTableName() {
        assertEquals("OUTBOX_EVENTS", EntityClassificationEntry.normalizeTableName("outbox_events"));
    }

    @Test
    void loadFailsClosedOnMalformedYaml() {
        assertThrows(
                EntityClassificationManifest.ManifestParseException.class,
                () -> parse("entries: [not-a-map]"));
    }

    private static EntityClassificationManifest parse(String yaml) {
        return EntityClassificationManifest.parse(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }
}
