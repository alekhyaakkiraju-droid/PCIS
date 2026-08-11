package com.pcis.config.codetable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pcis.config.CodeDomain;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CodeTableCompletenessGateTest {

  @Test
  void everyReferencedCodeDomainHasSeededRows() throws IOException {
    Set<CodeDomain> referenced = EnumSet.allOf(CodeDomain.class);
    Map<String, Set<String>> seeded = loadAllSeedDomains();

    CodeTableCompletenessChecker.CompletenessReport report =
        CodeTableCompletenessChecker.checkCompleteness(referenced, seeded);

    assertTrue(
        report.passed(),
        () -> CodeTableCompletenessChecker.formatFailureMessage(report));
  }

  @Test
  void detectsMissingDomainSeeds() {
    Map<String, Set<String>> seeded =
        Map.of("BILL_FREQ", Set.of("M"), "CLAIM_TYPE", Set.of("AUTO"));

    CodeTableCompletenessChecker.CompletenessReport report =
        CodeTableCompletenessChecker.checkCompleteness(EnumSet.allOf(CodeDomain.class), seeded);

    assertThat(report.passed()).isFalse();
    assertThat(report.missingDomains()).contains("CANCEL_REASON", "RESERVE_STATUS");
  }

  @Test
  void referencedDomainsAreDeclaredInCodeDomainRegistry() {
    Set<CodeDomain> referenced = EnumSet.allOf(CodeDomain.class);

    assertThat(referenced)
        .contains(
            CodeDomain.BILL_FREQ,
            CodeDomain.BILL_SCHED_STATUS,
            CodeDomain.CANCEL_REASON);
  }

  private static Map<String, Set<String>> loadAllSeedDomains() throws IOException {
    Map<String, Set<String>> merged = new java.util.HashMap<>();
    for (Path seedFile : seedFiles()) {
      String sql = Files.readString(seedFile);
      CodeTableCompletenessChecker.parseSeededDomains(sql)
          .forEach(
              (domain, codes) ->
                  merged.merge(
                      domain,
                      codes,
                      (left, right) -> {
                        java.util.Set<String> combined = new java.util.HashSet<>(left);
                        combined.addAll(right);
                        return combined;
                      }));
    }
    return merged;
  }

  private static java.util.List<Path> seedFiles() {
    Path schemaSeed =
        Path.of("../pcis-schema/db/migration/V4__seed_reference_data.sql")
            .toAbsolutePath()
            .normalize();
    if (!schemaSeed.toFile().exists()) {
      schemaSeed =
          Path.of("shared-libs", "pcis-schema", "db", "migration", "V4__seed_reference_data.sql")
              .toAbsolutePath()
              .normalize();
    }
    if (!schemaSeed.toFile().exists()) {
      schemaSeed =
          Path.of("..", "..", "pcis-schema", "db", "migration", "V4__seed_reference_data.sql")
              .toAbsolutePath()
              .normalize();
    }
    return java.util.List.of(schemaSeed);
  }
}
