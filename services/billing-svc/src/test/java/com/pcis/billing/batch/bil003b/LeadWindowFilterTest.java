package com.pcis.billing.batch.bil003b;

import static org.assertj.core.api.Assertions.assertThat;

import com.pcis.billing.batch.bil003b.domain.LeadWindowFilter;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LeadWindowFilterTest {

  private static final LocalDate REF = LocalDate.parse("2024-06-15");

  @Test
  void dayZeroIsIncluded() {
    assertThat(LeadWindowFilter.isEligible(REF, REF, 15)).isTrue();
    assertThat(LeadWindowFilter.isEligible(0, 15)).isTrue();
  }

  @Test
  void dayFifteenIsIncluded() {
    LocalDate due = REF.plusDays(15);
    assertThat(LeadWindowFilter.isEligible(due, REF, 15)).isTrue();
    assertThat(LeadWindowFilter.isEligible(15, 15)).isTrue();
  }

  @Test
  void daySixteenIsExcluded() {
    LocalDate due = REF.plusDays(16);
    assertThat(LeadWindowFilter.isEligible(due, REF, 15)).isFalse();
    assertThat(LeadWindowFilter.isEligible(16, 15)).isFalse();
  }
}
