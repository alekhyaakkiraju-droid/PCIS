package com.pcis.config.codetable;

import com.pcis.config.CodeDomain;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Build-time gate ensuring every referenced code domain has seeded active rows. */
public final class CodeTableCompletenessChecker {

  private static final Pattern DOMAIN_CODE_PATTERN =
      Pattern.compile("\\('([A-Z0-9_]+)'\\s*,\\s*'([A-Z0-9_]+)'");

  private CodeTableCompletenessChecker() {}

  public static CompletenessReport checkCompleteness(
      Set<CodeDomain> referencedDomains, Map<String, Set<String>> seededDomains) {
    List<String> missingDomains = new ArrayList<>();
    for (CodeDomain domain : referencedDomains) {
      Set<String> codes = seededDomains.get(domain.domainCode());
      if (codes == null || codes.isEmpty()) {
        missingDomains.add(domain.domainCode());
      }
    }
    missingDomains.sort(String::compareTo);
    return new CompletenessReport(List.copyOf(missingDomains), missingDomains.isEmpty());
  }

  public static Set<CodeDomain> referencedDomainsFromSources(String javaSource) {
    Set<CodeDomain> referenced = EnumSet.noneOf(CodeDomain.class);
    for (CodeDomain domain : CodeDomain.values()) {
      if (javaSource.contains("CodeDomain." + domain.name())
          || javaSource.contains("\"" + domain.domainCode() + "\"")) {
        referenced.add(domain);
      }
    }
    return referenced;
  }

  public static Map<String, Set<String>> parseSeededDomains(String seedSql) {
    Map<String, Set<String>> seeded = new HashMap<>();
    Set<String> knownDomains =
        java.util.Arrays.stream(CodeDomain.values()).map(CodeDomain::domainCode).collect(java.util.stream.Collectors.toSet());
    Matcher tupleMatcher = DOMAIN_CODE_PATTERN.matcher(seedSql);
    while (tupleMatcher.find()) {
      String domainCode = tupleMatcher.group(1);
      if (!knownDomains.contains(domainCode)) {
        continue;
      }
      String codeValue = tupleMatcher.group(2);
      seeded.computeIfAbsent(domainCode, ignored -> new java.util.HashSet<>()).add(codeValue);
    }
    return seeded;
  }

  public static String formatFailureMessage(CompletenessReport report) {
    return "Code-table completeness gate failed:\nMissing seeded domains: " + report.missingDomains();
  }

  public record CompletenessReport(List<String> missingDomains, boolean passed) {}
}
