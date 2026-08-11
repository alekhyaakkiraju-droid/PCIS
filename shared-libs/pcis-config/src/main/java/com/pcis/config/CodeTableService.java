package com.pcis.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Typed code-table lookup port backed by Caffeine cache (WO-061 pattern). */
public class CodeTableService {

  private static final Logger log = LoggerFactory.getLogger(CodeTableService.class);

  private final CodeTableRepository repository;
  private final PcisCodeTableProperties properties;
  private final Cache<String, Object> cache;

  public CodeTableService(CodeTableRepository repository, PcisCodeTableProperties properties) {
    this.repository = repository;
    this.properties = properties;
    this.cache =
        Caffeine.newBuilder()
            .maximumSize(properties.getCache().getMaxSize())
            .expireAfterWrite(Duration.ofSeconds(properties.getCache().getTtlSeconds()))
            .build();
  }

  public CodeTableEntry lookup(CodeDomain domain, String codeValue) {
    Objects.requireNonNull(domain, "domain");
    Objects.requireNonNull(codeValue, "codeValue");
    String cacheKey = entryKey(domain.domainCode(), codeValue);
    CodeTableEntry entry =
        (CodeTableEntry) cache.get(cacheKey, key -> repository.findByDomainAndCode(domain.domainCode(), codeValue));
    if (entry == null || !entry.active()) {
      throw new UnknownCodeValueException(domain.domainCode(), codeValue);
    }
    return entry;
  }

  public List<CodeTableEntry> listByDomain(CodeDomain domain) {
    Objects.requireNonNull(domain, "domain");
    String cacheKey = domainKey(domain.domainCode());
    @SuppressWarnings("unchecked")
    List<CodeTableEntry> entries =
        (List<CodeTableEntry>) cache.get(cacheKey, key -> List.copyOf(repository.findActiveByDomain(domain.domainCode())));
    return entries;
  }

  public boolean validateMembership(CodeDomain domain, String codeValue) {
    Objects.requireNonNull(domain, "domain");
    Objects.requireNonNull(codeValue, "codeValue");
    String cacheKey = membershipKey(domain.domainCode(), codeValue);
    Boolean member =
        (Boolean)
            cache.get(
                cacheKey,
                key -> repository.isActiveMember(domain.domainCode(), codeValue));
    return Boolean.TRUE.equals(member);
  }

  public void refresh(CodeDomain domain) {
    cache.invalidate(domainKey(domain.domainCode()));
    log.info(
        "Refreshed code-table cache actor=system resource=config/code-table/{} operation=refresh",
        domain.domainCode());
  }

  public void refresh(CodeDomain domain, String codeValue) {
    cache.invalidate(entryKey(domain.domainCode(), codeValue));
    cache.invalidate(membershipKey(domain.domainCode(), codeValue));
    cache.invalidate(domainKey(domain.domainCode()));
    log.info(
        "Refreshed code-table cache actor=system resource=config/code-table/{}/{} operation=refresh",
        domain.domainCode(),
        codeValue);
  }

  public void refreshAll() {
    cache.invalidateAll();
    log.info("Refreshed all code-table cache entries actor=system resource=config/code-table operation=refresh-all");
  }

  private static String entryKey(String domainCode, String codeValue) {
    return "entry:" + domainCode + ":" + codeValue;
  }

  private static String domainKey(String domainCode) {
    return "domain:" + domainCode;
  }

  private static String membershipKey(String domainCode, String codeValue) {
    return "member:" + domainCode + ":" + codeValue;
  }
}
