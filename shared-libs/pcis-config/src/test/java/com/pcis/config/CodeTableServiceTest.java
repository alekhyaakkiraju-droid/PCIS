package com.pcis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CodeTableServiceTest {

  private CodeTableRepository repository;
  private CodeTableService service;

  @BeforeEach
  void setUp() {
    repository = mock(CodeTableRepository.class);
    service = new CodeTableService(repository, new PcisCodeTableProperties());
  }

  @Test
  void lookupReturnsActiveCode() {
    when(repository.findByDomainAndCode("BILL_SCHED_STATUS", "V"))
        .thenReturn(new CodeTableEntry("BILL_SCHED_STATUS", "V", "Void", true));

    CodeTableEntry entry = service.lookup(CodeDomain.BILL_SCHED_STATUS, "V");

    assertThat(entry.description()).isEqualTo("Void");
  }

  @Test
  void lookupRejectsInactiveCode() {
    when(repository.findByDomainAndCode("CLAIM_TYPE", "AUTO"))
        .thenReturn(new CodeTableEntry("CLAIM_TYPE", "AUTO", "Automobile claim", false));

    assertThatThrownBy(() -> service.lookup(CodeDomain.CLAIM_TYPE, "AUTO"))
        .isInstanceOf(UnknownCodeValueException.class);
  }

  @Test
  void listByDomainReturnsActiveCodes() {
    when(repository.findActiveByDomain("RESERVE_STATUS"))
        .thenReturn(
            List.of(
                new CodeTableEntry("RESERVE_STATUS", "AP", "Approved", true),
                new CodeTableEntry("RESERVE_STATUS", "PD", "Paid", true)));

    assertThat(service.listByDomain(CodeDomain.RESERVE_STATUS))
        .extracting(CodeTableEntry::codeValue)
        .containsExactly("AP", "PD");
  }

  @Test
  void validateMembershipUsesCachedRepositoryResult() {
    when(repository.isActiveMember("CANCEL_REASON", "NPAY")).thenReturn(true);

    assertThat(service.validateMembership(CodeDomain.CANCEL_REASON, "NPAY")).isTrue();
  }

  @Test
  void refreshForcesReload() {
    when(repository.findByDomainAndCode("BILL_FREQ", "M"))
        .thenReturn(new CodeTableEntry("BILL_FREQ", "M", "Monthly billing", true))
        .thenReturn(new CodeTableEntry("BILL_FREQ", "M", "Monthly billing (updated)", true));

    assertThat(service.lookup(CodeDomain.BILL_FREQ, "M").description()).isEqualTo("Monthly billing");
    service.refresh(CodeDomain.BILL_FREQ, "M");
    assertThat(service.lookup(CodeDomain.BILL_FREQ, "M").description())
        .isEqualTo("Monthly billing (updated)");
  }
}
