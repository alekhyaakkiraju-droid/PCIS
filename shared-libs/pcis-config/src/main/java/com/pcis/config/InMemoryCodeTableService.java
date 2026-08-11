package com.pcis.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory CodeTableService stub for domain unit tests without a database. */
public final class InMemoryCodeTableService extends CodeTableService {

  public InMemoryCodeTableService(Map<CodeDomain, List<CodeTableEntry>> seedData) {
    super(new StubRepository(seedData), new PcisCodeTableProperties());
  }

  public static InMemoryCodeTableService fromEntries(CodeTableEntry... entries) {
    Map<CodeDomain, List<CodeTableEntry>> byDomain = new HashMap<>();
    for (CodeTableEntry entry : entries) {
      CodeDomain domain = CodeDomain.fromDomainCode(entry.domainCode());
      byDomain.computeIfAbsent(domain, ignored -> new java.util.ArrayList<>()).add(entry);
    }
    Map<CodeDomain, List<CodeTableEntry>> frozen = new HashMap<>();
    byDomain.forEach((domain, list) -> frozen.put(domain, List.copyOf(list)));
    return new InMemoryCodeTableService(frozen);
  }

  private static final class StubRepository extends CodeTableRepository {
    private final Map<String, Map<String, CodeTableEntry>> data = new HashMap<>();

    StubRepository(Map<CodeDomain, List<CodeTableEntry>> seedData) {
      super(null);
      seedData.forEach(
          (domain, entries) ->
              entries.forEach(
                  entry ->
                      data
                          .computeIfAbsent(domain.domainCode(), ignored -> new HashMap<>())
                          .put(entry.codeValue(), entry)));
    }

    @Override
    public CodeTableEntry findByDomainAndCode(String domainCode, String codeValue) {
      Map<String, CodeTableEntry> domainEntries = data.get(domainCode);
      return domainEntries == null ? null : domainEntries.get(codeValue);
    }

    @Override
    public List<CodeTableEntry> findActiveByDomain(String domainCode) {
      Map<String, CodeTableEntry> domainEntries = data.get(domainCode);
      if (domainEntries == null) {
        return List.of();
      }
      return domainEntries.values().stream().filter(CodeTableEntry::active).toList();
    }

    @Override
    public boolean isActiveMember(String domainCode, String codeValue) {
      CodeTableEntry entry = findByDomainAndCode(domainCode, codeValue);
      return entry != null && entry.active();
    }
  }
}
